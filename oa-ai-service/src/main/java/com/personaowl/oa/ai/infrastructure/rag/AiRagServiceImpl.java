package com.personaowl.oa.ai.infrastructure.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AiRagServiceImpl implements AiRagService {

    private static final Logger log = LoggerFactory.getLogger(AiRagServiceImpl.class);
    private static final String FALLBACK_ANSWER =
            "暂时无法生成回答。涉及公司具体制度时，建议以最新制度文件或管理员说明为准。";

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final AiRagProperties properties;

    public AiRagServiceImpl(
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder,
            AiRagProperties properties) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public RagResult answer(String question, String knowledgeDomain, Integer topK) {
        PromptData promptData = buildPromptData(question, knowledgeDomain, topK);
        String answer = generateAnswer(promptData);
        return toResult(answer, promptData.documents());
    }

    @Override
    public StreamResult answerStream(String question, String knowledgeDomain, Integer topK) {
        PromptData promptData = buildPromptData(question, knowledgeDomain, topK);
        List<Document> documents = promptData.documents();
        Flux<String> content = chatClient.prompt(toPrompt(promptData))
                .stream()
                .content()
                .filter(token -> token != null && !token.isEmpty())
                .switchIfEmpty(Flux.just(FALLBACK_ANSWER))
                .onErrorResume(ex -> {
            log.warn("AI streaming invocation failed, returning fallback answer: {}", ex.getMessage());
            return Flux.just(FALLBACK_ANSWER);
        });
        return new StreamResult(content, !documents.isEmpty(), toCitations(documents),
                toMatchedDocs(documents), confidence(documents));
    }

    private String generateAnswer(PromptData promptData) {
        try {
            String answer = chatClient.prompt(toPrompt(promptData)).call().content();
            return answer == null || answer.isBlank() ? FALLBACK_ANSWER : answer;
        } catch (RuntimeException ex) {
            log.warn("AI model invocation failed, returning fallback answer: {}", ex.getMessage());
            return FALLBACK_ANSWER;
        }
    }

    private Prompt toPrompt(PromptData promptData) {
        return new Prompt(new SystemMessage(promptData.system()), new UserMessage(promptData.user()));
    }

    private RagResult toResult(String answer, List<Document> documents) {
        boolean hit = !documents.isEmpty();
        return new RagResult(answer, hit, toCitations(documents), toMatchedDocs(documents), confidence(documents));
    }

    private List<Citation> toCitations(List<Document> documents) {
        return documents.stream()
                .map(doc -> new Citation(
                        toLong(doc.getMetadata().get("docId")),
                        string(doc.getMetadata().get("docTitle")),
                        toLong(doc.getMetadata().get("chunkId")),
                        toInt(doc.getMetadata().get("chunkNo")),
                        doc.getText(),
                        score(doc)))
                .toList();
    }

    private List<MatchedDoc> toMatchedDocs(List<Document> documents) {
        return documents.stream()
                .map(doc -> new MatchedDoc(
                        toLong(doc.getMetadata().get("docId")),
                        string(doc.getMetadata().get("docTitle")),
                        string(doc.getMetadata().get("docVersion"))))
                .distinct()
                .toList();
    }

    private double confidence(List<Document> documents) {
        return documents.stream().mapToDouble(this::score).max().orElse(0.0d);
    }

    private double score(Document document) {
        return document.getScore() == null ? 0.0d : document.getScore();
    }

    private PromptData buildPromptData(String question, String knowledgeDomain, Integer topK) {
        int k = Math.min(20, topK == null ? Math.max(1, properties.topK()) : Math.max(1, topK));
        SearchRequest.Builder request = SearchRequest.builder()
                .query(question)
                .topK(k)
                .similarityThreshold(Math.max(0.0d, Math.min(1.0d, properties.minScore())));
        if (knowledgeDomain != null && !knowledgeDomain.isBlank() && !"ALL".equalsIgnoreCase(knowledgeDomain)) {
            request.filterExpression(new FilterExpressionBuilder()
                    .eq("docDomain", knowledgeDomain.trim().toUpperCase())
                    .build());
        }
        List<Document> documents = Optional.ofNullable(
                        vectorStore.similaritySearch(request.build()))
                .orElseGet(List::of);
        String context = documents.stream()
                .map(doc -> "[" + doc.getMetadata().getOrDefault("docTitle", "")
                        + "|" + doc.getMetadata().getOrDefault("docVersion", "")
                        + "] " + doc.getText())
                .collect(Collectors.joining("\n\n"));
        String user = "问题：" + question + "\n\n" + (documents.isEmpty()
                ? "知识库中未检索到直接相关内容。请基于通用办公知识回答，并说明具体公司规定应以最新制度为准。"
                : "可参考的知识库上下文：\n" + context);
        return new PromptData(buildSystemPrompt(), user, documents);
    }

    private String buildSystemPrompt() {
        return "你是专业、友好的OA办公助手。优先参考提供的知识库上下文，以确保涉及本公司制度、流程、期限、审批规则等内容准确。"
                + "对于上下文未覆盖的通用办公问题，可以结合通用知识给出自然、实用的建议；对于无法确认的公司具体规定，要明确说明该部分需要以公司最新制度或管理员说明为准。"
                + "不要杜撰具体的公司制度、金额、时间、审批人或政策条款。回答应直接、清晰，必要时使用分点说明。";
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

    private record PromptData(String system, String user, List<Document> documents) {}
}
