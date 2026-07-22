package com.personaowl.oa.ai.infrastructure.rag;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpDeepSeekChatGateway implements DeepSeekChatGateway {

    private final RestClient restClient;
    private final AiRagProperties properties;

    public HttpDeepSeekChatGateway(AiRagProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.chatModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("temperature", 0.2);
        body.put("stream", false);

        Map response = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            return "";
        }
        List choices = (List) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        Map first = (Map) choices.get(0);
        Map message = (Map) first.get("message");
        return message == null ? "" : String.valueOf(message.getOrDefault("content", ""));
    }
}
