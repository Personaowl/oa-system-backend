package com.personaowl.oa.notice.domain.vo;

import java.time.LocalDateTime;

public class NoticeListItemVO {
    private Long id;
    private String title;
    private String summary;
    private String status;
    private Boolean topFlag;
    private LocalDateTime publishedAt;
    private Long viewCount;
    private boolean read;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getTopFlag() {
        return topFlag;
    }

    public void setTopFlag(Boolean topFlag) {
        this.topFlag = topFlag;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
