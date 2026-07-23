package com.personaowl.oa.ai.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AiChatLog {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String question;
    private String answer;
    private String knowledgeDomain;
    private String retrievedDocIds;
    private String retrievedChunkIds;
    private String citationsJson;
    private String modelName;
    private Integer topK;
    private BigDecimal confidenceScore;
    private Integer hitFlag;
    private Integer latencyMs;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getKnowledgeDomain() { return knowledgeDomain; }
    public void setKnowledgeDomain(String knowledgeDomain) { this.knowledgeDomain = knowledgeDomain; }
    public String getRetrievedDocIds() { return retrievedDocIds; }
    public void setRetrievedDocIds(String retrievedDocIds) { this.retrievedDocIds = retrievedDocIds; }
    public String getRetrievedChunkIds() { return retrievedChunkIds; }
    public void setRetrievedChunkIds(String retrievedChunkIds) { this.retrievedChunkIds = retrievedChunkIds; }
    public String getCitationsJson() { return citationsJson; }
    public void setCitationsJson(String citationsJson) { this.citationsJson = citationsJson; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public Integer getHitFlag() { return hitFlag; }
    public void setHitFlag(Integer hitFlag) { this.hitFlag = hitFlag; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
