package com.personaowl.oa.ai.infrastructure.rag;

public record AiVectorDocument(
        Long docId,
        String docTitle,
        String docDomain,
        String docVersion,
        Long chunkId,
        Integer chunkNo,
        String snippet,
        double score) {
}
