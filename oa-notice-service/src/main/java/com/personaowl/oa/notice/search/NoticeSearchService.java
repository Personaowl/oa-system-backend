package com.personaowl.oa.notice.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.personaowl.oa.notice.domain.dto.NoticeSearchRequest;
import com.personaowl.oa.notice.domain.entity.Notice;
import com.personaowl.oa.notice.domain.vo.NoticePageVO;
import com.personaowl.oa.notice.domain.vo.NoticeSearchItemVO;
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
import java.util.List;
import java.util.Map;

@Component
public class NoticeSearchService {
    public static final String INDEX_NAME = "oa_notice_v1";
    public static final String HIGHLIGHT_START = "[[[H]]]";
    public static final String HIGHLIGHT_END = "[[[/H]]]";
    private static final Logger log = LoggerFactory.getLogger(NoticeSearchService.class);
    private static final String INDEX_DEFINITION = """
            {
              "settings": { "number_of_shards": 1, "number_of_replicas": 0 },
              "mappings": {
                "dynamic": "strict",
                "properties": {
                  "id": { "type": "keyword" },
                  "title": { "type": "text", "analyzer": "standard" },
                  "summary": { "type": "text", "analyzer": "standard" },
                  "content": { "type": "text", "analyzer": "standard" },
                  "publisherId": { "type": "keyword" },
                  "status": { "type": "keyword" },
                  "topFlag": { "type": "boolean" },
                  "publishedAt": { "type": "date" },
                  "createdAt": { "type": "date" },
                  "updatedAt": { "type": "date" },
                  "viewCount": { "type": "long" },
                  "deleted": { "type": "boolean" }
                }
              }
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public NoticeSearchService(@Value("${spring.elasticsearch.uris:http://127.0.0.1:9200}") String uris,
                               ObjectMapper objectMapper) {
        String endpoint = uris.split(",")[0].trim();
        this.restClient = RestClient.builder().baseUrl(endpoint).build();
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeIndex() {
        try {
            ensureIndex();
        } catch (RuntimeException exception) {
            log.warn("Elasticsearch notice index initialization skipped: {}", exception.getMessage());
        }
    }

    public void synchronizeAfterCommit(Notice notice) {
        Map<String, Object> document = toDocument(notice);
        runAfterCommit(() -> executeSafely("index notice " + notice.getId(), () -> index(document, notice.getId())));
    }

    public void deleteAfterCommit(Long noticeId) {
        runAfterCommit(() -> executeSafely("delete notice " + noticeId, () -> deleteDocument(noticeId)));
    }

    public int rebuild(List<Notice> notices) {
        recreateIndex();
        int count = 0;
        for (Notice notice : notices) {
            if (!Integer.valueOf(1).equals(notice.getDeleted())) {
                index(toDocument(notice), notice.getId());
                count++;
            }
        }
        restClient.post().uri("/{index}/_refresh", INDEX_NAME).retrieve().toBodilessEntity();
        return count;
    }

    public NoticePageVO<NoticeSearchItemVO> search(NoticeSearchRequest request, boolean publishedOnly) {
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
            multiMatch.put("query", keyword);
            multiMatch.put("operator", "and");
            multiMatch.put("type", "best_fields");
            multiMatch.putArray("fields").add("title^5").add("summary^2").add("content");
        }

        ArrayNode filters = bool.putArray("filter");
        filters.addObject().putObject("term").put("deleted", false);
        String status = publishedOnly ? "PUBLISHED" : normalize(request.getStatus());
        if (status != null) filters.addObject().putObject("term").put("status", status);
        if (request.getTopFlag() != null) filters.addObject().putObject("term").put("topFlag", request.getTopFlag());
        if (request.getPublishedFrom() != null || request.getPublishedTo() != null) {
            ObjectNode range = filters.addObject().putObject("range").putObject("publishedAt");
            if (request.getPublishedFrom() != null) range.put("gte", request.getPublishedFrom().toString());
            if (request.getPublishedTo() != null) range.put("lte", request.getPublishedTo().toString());
        }

        ArrayNode sort = body.putArray("sort");
        sort.addObject().putObject("topFlag").put("order", "desc");
        if (keyword != null) sort.addObject().putObject("_score").put("order", "desc");
        sort.addObject().putObject("publishedAt").put("order", "desc").put("missing", "_last");
        sort.addObject().putObject("updatedAt").put("order", "desc");

        if (keyword != null) {
            ObjectNode highlight = body.putObject("highlight");
            highlight.putArray("pre_tags").add(HIGHLIGHT_START);
            highlight.putArray("post_tags").add(HIGHLIGHT_END);
            highlight.put("require_field_match", false);
            ObjectNode fields = highlight.putObject("fields");
            fields.putObject("title").put("number_of_fragments", 0);
            fields.putObject("summary").put("fragment_size", 120).put("number_of_fragments", 1);
            fields.putObject("content").put("fragment_size", 180).put("number_of_fragments", 1);
        }

        JsonNode response = restClient.post().uri("/{index}/_search", INDEX_NAME)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        if (response == null) return new NoticePageVO<>(0, List.of());
        long total = response.path("hits").path("total").path("value").asLong();
        List<NoticeSearchItemVO> records = new java.util.ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) records.add(toSearchItem(hit));
        return new NoticePageVO<>(total, records);
    }

    private NoticeSearchItemVO toSearchItem(JsonNode hit) {
        JsonNode source = hit.path("_source");
        NoticeSearchItemVO item = new NoticeSearchItemVO();
        item.setId(Long.valueOf(source.path("id").asText()));
        item.setTitle(source.path("title").asText(""));
        item.setSummary(nullIfMissing(source, "summary"));
        String content = nullIfMissing(source, "content");
        item.setContentSnippet(content == null || content.length() <= 180 ? content : content.substring(0, 180) + "…");
        item.setStatus(source.path("status").asText(""));
        item.setTopFlag(source.path("topFlag").asBoolean(false));
        item.setPublishedAt(parseDate(nullIfMissing(source, "publishedAt")));
        item.setViewCount(source.path("viewCount").asLong(0));
        item.setHighlightedTitle(firstHighlight(hit, "title"));
        item.setHighlightedSummary(firstHighlight(hit, "summary"));
        item.setHighlightedContent(firstHighlight(hit, "content"));
        return item;
    }

    private String firstHighlight(JsonNode hit, String field) {
        JsonNode values = hit.path("highlight").path(field);
        return values.isArray() && !values.isEmpty() ? values.get(0).asText() : null;
    }

    private void ensureIndex() {
        try {
            restClient.head().uri("/{index}", INDEX_NAME).retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) throw exception;
            createIndex();
        }
    }

    private void recreateIndex() {
        try {
            restClient.delete().uri("/{index}", INDEX_NAME).retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) throw exception;
        }
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

    private void deleteDocument(Long id) {
        try {
            restClient.delete().uri("/{index}/_doc/{id}", INDEX_NAME, id).retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) throw exception;
        }
    }

    private Map<String, Object> toDocument(Notice notice) {
        Map<String, Object> document = new java.util.LinkedHashMap<>();
        document.put("id", String.valueOf(notice.getId()));
        document.put("title", notice.getTitle());
        document.put("summary", notice.getSummary());
        document.put("content", notice.getContent());
        document.put("publisherId", notice.getPublisherId() == null ? null : String.valueOf(notice.getPublisherId()));
        document.put("status", notice.getStatus());
        document.put("topFlag", Boolean.TRUE.equals(notice.getTopFlag()));
        document.put("publishedAt", date(notice.getPublishedAt()));
        document.put("createdAt", date(notice.getCreatedAt()));
        document.put("updatedAt", date(notice.getUpdatedAt()));
        document.put("viewCount", notice.getViewCount() == null ? 0 : notice.getViewCount());
        document.put("deleted", Integer.valueOf(1).equals(notice.getDeleted()));
        return document;
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

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String date(LocalDateTime value) { return value == null ? null : value.toString(); }
    private LocalDateTime parseDate(String value) { return value == null ? null : LocalDateTime.parse(value); }
    private String nullIfMissing(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
