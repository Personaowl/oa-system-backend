package com.personaowl.oa.ai.infrastructure.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnBean(VectorStore.class)
public class RedisAiVectorStoreGateway implements AiVectorStoreGateway {

    private final VectorStore vectorStore;

    public RedisAiVectorStoreGateway(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void upsert(Long docId, String docTitle, String docVersion, List<String> chunks) {
        deleteByDocId(docId);
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            documents.add(new Document(
                    chunks.get(i),
                    Map.of(
                            "docId", String.valueOf(docId),
                            "docTitle", docTitle,
                            "docVersion", docVersion,
                            "chunkId", String.valueOf(docId * 1000 + i + 1),
                            "chunkNo", String.valueOf(i + 1)
                    )));
        }
        vectorStore.add(documents);
    }

    @Override
    public List<AiVectorDocument> search(String question, String knowledgeDomain, int topK) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(Math.max(1, topK)).build());
        return documents.stream().map(doc -> new AiVectorDocument(
                toLong(doc.getMetadata().get("docId")),
                string(doc.getMetadata().get("docTitle")),
                string(doc.getMetadata().get("docVersion")),
                toLong(doc.getMetadata().get("chunkId")),
                toInt(doc.getMetadata().get("chunkNo")),
                doc.getText(),
                0.9d
        )).toList();
    }

    @Override
    public void deleteByDocId(Long docId) {
        vectorStore.delete(List.of("docId:" + docId));
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return 0L;
        }
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
