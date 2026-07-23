package com.personaowl.oa.ai.infrastructure.rag;

import java.util.List;

public interface AiVectorStoreGateway {

    void upsert(Long docId, String docTitle, String docDomain, String docVersion, List<String> chunks);

    List<AiVectorDocument> search(String question, String knowledgeDomain, int topK);

    void deleteByDocId(Long docId);
}
