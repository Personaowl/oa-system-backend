package com.personaowl.oa.ai.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record AiChatResponseVO(
        Long sessionId,
        String answer,
        Boolean hitFlag,
        List<MatchedDocVO> matchedDocs,
        List<CitationVO> citations,
        String traceId,
        OffsetDateTime timestamp) {

    public static AiChatResponseVO empty() {
        return new AiChatResponseVO(null, null, null, List.of(), List.of(), null, null);
    }

    public record MatchedDocVO(Long docId, String docTitle, String docVersion) {
    }

    public record CitationVO(Long docId, String docTitle, Long chunkId, Integer chunkNo, String snippet, Double score) {
    }
}
