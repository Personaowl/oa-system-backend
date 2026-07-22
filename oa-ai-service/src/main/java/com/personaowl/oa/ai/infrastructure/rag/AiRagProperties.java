package com.personaowl.oa.ai.infrastructure.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.rag")
public record AiRagProperties(
        int topK,
        double minScore,
        int maxChunkSize,
        int chunkOverlap,
        String knowledgePrefix,
        String indexName,
        String chatModel,
        String embeddingModel,
        String baseUrl,
        String apiKey,
        String redisHost,
        int redisPort) {
}
