package com.personaowl.oa.ai.infrastructure.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
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
        return buildRagResult(question, knowledgeDomain, topK);
    }

    @Override
    public Flux<String> answerStream(String question, String knowledgeDomain, Integer topK) {
        PromptData promptData = buildPromptData(question, knowledgeDomain, topK);
        return chatClient.prompt(new Prompt(new SystemMessage(promptData.system()), new UserMessage(promptData.user())))
                .stream()
                .content()
                .defaultIfEmpty("");
    }

    private RagResult buildRagResult(String question, String knowledgeDomain, Integer topK) {
        PromptData promptData = buildPromptData(question, knowledgeDomain, topK);
        int k = topK == null ? Math.max(1, properties.topK()) : topK;
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(k).build());
        boolean hit = !documents.isEmpty();
        String answer = chatClient.prompt(new Prompt(new SystemMessage(promptData.system()), new UserMessage(promptData.user())))
                .call()
                .content();
        if (answer == null || answer.isBlank()) {
            answer = "暂时无法生成回答。涉及公司具体制度时，建议以最新制度文件或管理员说明为准。";
        }
        List<Citation> citations = documents.stream()
                .map(doc -> new Citation(toLong(doc.getMetadata().get("docId")), string(doc.getMetadata().get("docTitle")), toLong(doc.getMetadata().get("chunkId")), toInt(doc.getMetadata().get("chunkNo")), doc.getText(), 0.9d))
                .toList();
        List<MatchedDoc> matchedDocs = documents.stream()
                .map(doc -> new MatchedDoc(toLong(doc.getMetadata().get("docId")), string(doc.getMetadata().get("docTitle")), string(doc.getMetadata().get("docVersion"))))
                .toList();
        return new RagResult(answer, hit, citations, matchedDocs, hit ? 0.85d : 0.0d);
    }

    private PromptData buildPromptData(String question, String knowledgeDomain, Integer topK) {
        int k = topK == null ? Math.max(1, properties.topK()) : topK;
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(k).build());
        boolean hit = !documents.isEmpty();
        String context = documents.stream().map(doc -> "[" + doc.getMetadata().getOrDefault("docTitle", "") + "|" + doc.getMetadata().getOrDefault("docVersion", "") + "] " + doc.getText()).collect(Collectors.joining("\n\n"));
        return new PromptData(buildSystemPrompt(), "问题：" + question + "\n\n" + (hit ? "可参考的知识库上下文：\n" + context : "知识库中未检索到直接相关内容。请基于通用办公知识回答，并说明具体公司规定应以最新制度为准。"));
    }

    private String buildSystemPrompt() {
        return "你是专业、友好的OA办公助手。优先参考提供的知识库上下文，以确保涉及本公司制度、流程、期限、审批规则等内容准确。" +
                "对于上下文未覆盖的通用办公问题，可以结合通用知识给出自然、实用的建议；对于无法确认的公司具体规定，要明确说明该部分需要以公司最新制度或管理员说明为准。" +
                "不要杜撰具体的公司制度、金额、时间、审批人或政策条款。回答应直接、清晰，必要时使用分点说明。";
    }

    private record PromptData(String system, String user) {}

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
