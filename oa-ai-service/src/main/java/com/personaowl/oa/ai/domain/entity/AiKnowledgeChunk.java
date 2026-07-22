package com.personaowl.oa.ai.domain.entity;

import java.time.LocalDateTime;

public class AiKnowledgeChunk {
    private Long id;
    private Long docId;
    private Integer chunkNo;
    private String chunkTitle;
    private String chunkText;
    private String chunkHash;
    private String vectorKey;
    private String metadataJson;
    private String embeddingModel;
    private Integer embeddingDim;
    private String status;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }
    public Integer getChunkNo() { return chunkNo; }
    public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
    public String getChunkTitle() { return chunkTitle; }
    public void setChunkTitle(String chunkTitle) { this.chunkTitle = chunkTitle; }
    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }
    public String getChunkHash() { return chunkHash; }
    public void setChunkHash(String chunkHash) { this.chunkHash = chunkHash; }
    public String getVectorKey() { return vectorKey; }
    public void setVectorKey(String vectorKey) { this.vectorKey = vectorKey; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public Integer getEmbeddingDim() { return embeddingDim; }
    public void setEmbeddingDim(Integer embeddingDim) { this.embeddingDim = embeddingDim; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
