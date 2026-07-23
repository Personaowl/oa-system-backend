package com.personaowl.oa.ai.infrastructure.rag;

import java.util.List;

public interface AiRagService {

    RagResult answer(String question, String knowledgeDomain, Integer topK);

    reactor.core.publisher.Flux<String> answerStream(String question, String knowledgeDomain, Integer topK);

    record RagResult(String answer, boolean hitFlag, List<Citation> citations, List<MatchedDoc> matchedDocs, double confidenceScore) {
    }

    record Citation(Long docId, String docTitle, Long chunkId, Integer chunkNo, String snippet, double score) {
    }

    record MatchedDoc(Long docId, String docTitle, String docVersion) {
    }
}
