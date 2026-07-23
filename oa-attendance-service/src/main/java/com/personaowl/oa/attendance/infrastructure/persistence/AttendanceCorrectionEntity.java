package com.personaowl.oa.attendance.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.personaowl.oa.attendance.domain.AttendanceCorrectionStatus;
import com.personaowl.oa.attendance.domain.AttendanceCorrectionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("attendance_correction")
public class AttendanceCorrectionEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private LocalDate workDate;
    private AttendanceCorrectionType correctionType;
    private LocalDateTime correctionTime;
    private String reason;
    private AttendanceCorrectionStatus status;
    private Long approverId;
    private String reviewComment;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public AttendanceCorrectionType getCorrectionType() { return correctionType; }
    public void setCorrectionType(AttendanceCorrectionType correctionType) { this.correctionType = correctionType; }
    public LocalDateTime getCorrectionTime() { return correctionTime; }
    public void setCorrectionTime(LocalDateTime correctionTime) { this.correctionTime = correctionTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public AttendanceCorrectionStatus getStatus() { return status; }
    public void setStatus(AttendanceCorrectionStatus status) { this.status = status; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
