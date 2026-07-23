package com.personaowl.oa.flow.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.personaowl.oa.flow.domain.dto.FlowSearchRequest;
import com.personaowl.oa.flow.domain.entity.FlowActionLog;
import com.personaowl.oa.flow.domain.entity.FlowRequest;
import com.personaowl.oa.flow.domain.vo.FlowSearchItemResponse;
import com.personaowl.oa.flow.domain.vo.FlowSearchPageResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FlowSearchService {
    public static final String INDEX_NAME = "oa_flow_v1";
    private static final String HIGHLIGHT_START = "[[[H]]]";
    private static final String HIGHLIGHT_END = "[[[/H]]]";
    private static final Logger log = LoggerFactory.getLogger(FlowSearchService.class);
    private static final String INDEX_DEFINITION = """
            {
              "settings": { "number_of_shards": 1, "number_of_replicas": 0 },
              "mappings": {
                "dynamic": "strict",
                "properties": {
                  "id": { "type": "keyword" },
                  "title": { "type": "text", "analyzer": "standard" },
                  "content": { "type": "text", "analyzer": "standard" },
                  "requestType": { "type": "keyword" },
                  "status": { "type": "keyword" },
                  "applicantId": { "type": "keyword" },
                  "currentApproverId": { "type": "keyword" },
                  "relatedUserIds": { "type": "keyword" },
                  "startTime": { "type": "date" },
                  "endTime": { "type": "date" },
                  "createdAt": { "type": "date" },
                  "updatedAt": { "type": "date" }
                }
              }
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FlowSearchService(@Value("${spring.elasticsearch.uris:http://127.0.0.1:9200}") String uris,
                             ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(uris.split(",")[0].trim()).build();
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeIndex() {
        try { ensureIndex(); }
        catch (RuntimeException exception) { log.warn("Elasticsearch flow index initialization skipped: {}", exception.getMessage()); }
    }

    public void synchronizeAfterCommit(FlowRequest request, FlowActionLog action) {
        Map<String, Object> document = toDocument(request, action);
        runAfterCommit(() -> executeSafely("index flow " + request.getId(), () -> index(document, request.getId())));
    }

    public int rebuild(List<IndexedFlow> flows) {
        recreateIndex();
        for (IndexedFlow flow : flows) index(toDocument(flow.request(), flow.latestAction()), flow.request().getId());
        restClient.post().uri("/{index}/_refresh", INDEX_NAME).retrieve().toBodilessEntity();
        return flows.size();
    }

    public FlowSearchPageResponse search(FlowSearchRequest request, Long currentUserId, boolean allVisible) {
        ensureIndex();
        int page = Math.max(1, request.getPage() == null ? 1 : request.getPage());
        int size = Math.min(100, Math.max(1, request.getSize() == null ? 20 : request.getSize()));
        ObjectNode body = objectMapper.createObjectNode();
        body.put("from", (page - 1) * size);
        body.put("size", size);
        ObjectNode bool = body.putObject("query").putObject("bool");
        ArrayNode must = bool.putArray("must");
        String keyword = normalize(request.getKeyword());
        if (keyword != null) {
            ObjectNode multiMatch = must.addObject().putObject("multi_match");
            multiMatch.put("query", keyword).put("operator", "and").put("type", "best_fields");
            multiMatch.putArray("fields").add("title^4").add("content");
        }
        ArrayNode filters = bool.putArray("filter");
        if (!allVisible) filters.addObject().putObject("term").put("relatedUserIds", String.valueOf(currentUserId));
        String status = normalize(request.getStatus());
        if (status != null) filters.addObject().putObject("term").put("status", status);
        String requestType = normalize(request.getRequestType());
        if (requestType != null) filters.addObject().putObject("term").put("requestType", requestType);
        if (request.getCreatedFrom() != null || request.getCreatedTo() != null) {
            ObjectNode range = filters.addObject().putObject("range").putObject("createdAt");
            if (request.getCreatedFrom() != null) range.put("gte", request.getCreatedFrom().toString());
            if (request.getCreatedTo() != null) range.put("lte", request.getCreatedTo().toString());
        }
        ArrayNode sort = body.putArray("sort");
        if (keyword != null) sort.addObject().putObject("_score").put("order", "desc");
        sort.addObject().putObject("createdAt").put("order", "desc");
        if (keyword != null) {
            ObjectNode highlight = body.putObject("highlight");
            highlight.putArray("pre_tags").add(HIGHLIGHT_START);
            highlight.putArray("post_tags").add(HIGHLIGHT_END);
            ObjectNode fields = highlight.putObject("fields");
            fields.putObject("title").put("number_of_fragments", 0);
            fields.putObject("content").put("fragment_size", 180).put("number_of_fragments", 1);
        }
        JsonNode response = restClient.post().uri("/{index}/_search", INDEX_NAME)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        if (response == null) return new FlowSearchPageResponse(0, List.of());
        long total = response.path("hits").path("total").path("value").asLong();
        List<FlowSearchItemResponse> records = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) records.add(toResponse(hit));
        return new FlowSearchPageResponse(total, records);
    }

    private FlowSearchItemResponse toResponse(JsonNode hit) {
        JsonNode source = hit.path("_source");
        String content = nullable(source, "content");
        String snippet = content == null || content.length() <= 180 ? content : content.substring(0, 180) + "…";
        return new FlowSearchItemResponse(
                Long.valueOf(source.path("id").asText()), source.path("title").asText(), snippet,
                source.path("requestType").asText(), source.path("status").asText(),
                longValue(source, "applicantId"), longValue(source, "currentApproverId"),
                date(source, "startTime"), date(source, "endTime"), date(source, "createdAt"), date(source, "updatedAt"),
                highlight(hit, "title"), highlight(hit, "content"));
    }

    private Map<String, Object> toDocument(FlowRequest request, FlowActionLog action) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", String.valueOf(request.getId()));
        document.put("title", title(request.getRequestType()));
        document.put("content", request.getReason());
        document.put("requestType", request.getRequestType());
        document.put("status", request.getStatus());
        document.put("applicantId", string(request.getApplicantId()));
        document.put("currentApproverId", string(request.getCurrentApproverId()));
        List<String> related = new ArrayList<>();
        addRelated(related, request.getApplicantId());
        addRelated(related, request.getCurrentApproverId());
        if (action != null) addRelated(related, action.getOperatorId());
        document.put("relatedUserIds", related);
        document.put("startTime", date(request.getStartTime()));
        document.put("endTime", date(request.getEndTime()));
        document.put("createdAt", date(request.getCreatedAt()));
        document.put("updatedAt", date(request.getUpdatedAt()));
        return document;
    }

    private void ensureIndex() {
        try { restClient.head().uri("/{index}", INDEX_NAME).retrieve().toBodilessEntity(); }
        catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) throw exception;
            createIndex();
        }
    }

    private void recreateIndex() {
        try { restClient.delete().uri("/{index}", INDEX_NAME).retrieve().toBodilessEntity(); }
        catch (RestClientResponseException exception) { if (exception.getStatusCode().value() != 404) throw exception; }
        createIndex();
    }

    private void createIndex() {
        restClient.put().uri("/{index}", INDEX_NAME).contentType(MediaType.APPLICATION_JSON)
                .body(INDEX_DEFINITION).retrieve().toBodilessEntity();
    }

    private void index(Map<String, Object> document, Long id) {
        ensureIndex();
        restClient.put().uri("/{index}/_doc/{id}", INDEX_NAME, id).contentType(MediaType.APPLICATION_JSON)
                .body(document).retrieve().toBodilessEntity();
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { task.run(); }
            });
        } else task.run();
    }

    private void executeSafely(String operation, Runnable task) {
        try { task.run(); }
        catch (RuntimeException exception) { log.warn("Elasticsearch {} failed: {}", operation, exception.getMessage()); }
    }

    private String title(String type) { return "LEAVE".equals(type) ? "请假申请" : "OVERTIME".equals(type) ? "加班申请" : "审批申请"; }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String string(Long value) { return value == null ? null : String.valueOf(value); }
    private String date(LocalDateTime value) { return value == null ? null : value.toString(); }
    private LocalDateTime date(JsonNode source, String field) { String value = nullable(source, field); return value == null ? null : LocalDateTime.parse(value); }
    private Long longValue(JsonNode source, String field) { String value = nullable(source, field); return value == null ? null : Long.valueOf(value); }
    private String nullable(JsonNode node, String field) { JsonNode value = node.get(field); return value == null || value.isNull() ? null : value.asText(); }
    private String highlight(JsonNode hit, String field) { JsonNode values = hit.path("highlight").path(field); return values.isArray() && !values.isEmpty() ? values.get(0).asText() : null; }
    private void addRelated(List<String> users, Long userId) { String value = string(userId); if (value != null && !users.contains(value)) users.add(value); }

    public record IndexedFlow(FlowRequest request, FlowActionLog latestAction) { }
}
