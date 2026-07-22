package com.personaowl.oa.ai.infrastructure.rag;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnMissingBean(VectorStore.class)
public class InMemoryAiVectorStoreGateway implements AiVectorStoreGateway {

    private final List<AiVectorDocument> store = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void upsert(Long docId, String docTitle, String docVersion, List<String> chunks) {
        deleteByDocId(docId);
        for (int i = 0; i < chunks.size(); i++) {
            store.add(new AiVectorDocument(docId, docTitle, docVersion, docId * 1000 + i + 1, i + 1, chunks.get(i), 0.9d));
        }
    }

    @Override
    public List<AiVectorDocument> search(String question, String knowledgeDomain, int topK) {
        return store.stream().limit(Math.max(1, topK)).toList();
    }

    @Override
    public void deleteByDocId(Long docId) {
        store.removeIf(item -> item.docId().equals(docId));
    }
}
