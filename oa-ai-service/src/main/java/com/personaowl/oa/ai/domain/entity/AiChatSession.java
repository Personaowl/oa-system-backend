package com.personaowl.oa.ai.domain.entity;

import java.time.LocalDateTime;

public class AiChatSession {
    private Long id;
    private String sessionNo;
    private Long userId;
    private String sessionTitle;
    private String knowledgeDomain;
    private String latestQuestion;
    private String latestAnswer;
    private Integer messageCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionNo() { return sessionNo; }
    public void setSessionNo(String sessionNo) { this.sessionNo = sessionNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }
    public String getKnowledgeDomain() { return knowledgeDomain; }
    public void setKnowledgeDomain(String knowledgeDomain) { this.knowledgeDomain = knowledgeDomain; }
    public String getLatestQuestion() { return latestQuestion; }
    public void setLatestQuestion(String latestQuestion) { this.latestQuestion = latestQuestion; }
    public String getLatestAnswer() { return latestAnswer; }
    public void setLatestAnswer(String latestAnswer) { this.latestAnswer = latestAnswer; }
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
