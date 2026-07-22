package com.personaowl.oa.ai.infrastructure.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiRagServiceImpl implements AiRagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final AiRagProperties properties;

    public AiRagServiceImpl(VectorStore vectorStore, ChatClient.Builder chatClientBuilder, AiRagProperties properties) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public RagResult answer(String question, String knowledgeDomain, Integer topK) {
        int k = topK == null ? Math.max(1, properties.topK()) : topK;
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(k).build());
        boolean hit = true;

        String context = documents.stream()
                .map(doc -> "[" + doc.getMetadata().getOrDefault("docTitle", "") + "|" + doc.getMetadata().getOrDefault("docVersion", "") + "] " + doc.getText())
                .collect(Collectors.joining("\n\n"));

        if (!hit) {
            return new RagResult("未检索到相关制度依据，请联系管理员确认文档是否已审核入库。", false, List.of(), List.of(), 0.0d);
        }

        String system = "你是OA办公制度问答助手。" +
                "如果无法从上下文确认答案，必须明确说明未检索到依据，不允许编造。" ;
        String user = "问题：" + question + "\n\n知识库上下文：\n" + context;

        String answer = chatClient.prompt(new Prompt(new SystemMessage(system), new UserMessage(user)))
                .call()
                .content();
        if (answer == null || answer.isBlank()) {
            answer = "未检索到相关制度依据，请联系管理员确认文档是否已审核入库。";
            hit = false;
        }

        List<Citation> citations = documents.stream()
                .map(doc -> new Citation(
                        toLong(doc.getMetadata().get("docId")),
                        string(doc.getMetadata().get("docTitle")),
                        toLong(doc.getMetadata().get("chunkId")),
                        toInt(doc.getMetadata().get("chunkNo")),
                        doc.getText(),
                        0.9d))
                .toList();
        List<MatchedDoc> matchedDocs = documents.stream()
                .map(doc -> new MatchedDoc(toLong(doc.getMetadata().get("docId")), string(doc.getMetadata().get("docTitle")), string(doc.getMetadata().get("docVersion"))))
                .toList();

        return new RagResult(answer, hit, citations, matchedDocs, hit ? 0.85d : 0.0d);
    }

    private Long toLong(Object value) {
        try {
            return value == null ? 0L : Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return 0L;
        }
    }

    private Integer toInt(Object value) {
        try {
            return value == null ? 0 : Integer.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
