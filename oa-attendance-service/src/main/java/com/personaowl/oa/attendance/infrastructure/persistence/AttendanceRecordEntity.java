package com.personaowl.oa.attendance.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.personaowl.oa.attendance.domain.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@TableName("attendance_record")
public class AttendanceRecordEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private LocalDate workDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private AttendanceStatus status;
    private int lateMinutes;
    private int earlyLeaveMinutes;
    private int actualWorkMinutes;
    private LocalTime ruleWorkStart;
    private LocalTime ruleWorkEnd;
    private Integer ruleLateThresholdMinutes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public int getLateMinutes() {
        return lateMinutes;
    }

    public void setLateMinutes(int lateMinutes) {
        this.lateMinutes = lateMinutes;
    }

    public int getEarlyLeaveMinutes() {
        return earlyLeaveMinutes;
    }

    public void setEarlyLeaveMinutes(int earlyLeaveMinutes) {
        this.earlyLeaveMinutes = earlyLeaveMinutes;
    }

    public int getActualWorkMinutes() {
        return actualWorkMinutes;
    }

    public void setActualWorkMinutes(int actualWorkMinutes) {
        this.actualWorkMinutes = actualWorkMinutes;
    }

    public LocalTime getRuleWorkStart() {
        return ruleWorkStart;
    }

    public void setRuleWorkStart(LocalTime ruleWorkStart) {
        this.ruleWorkStart = ruleWorkStart;
    }

    public LocalTime getRuleWorkEnd() {
        return ruleWorkEnd;
    }

    public void setRuleWorkEnd(LocalTime ruleWorkEnd) {
        this.ruleWorkEnd = ruleWorkEnd;
    }

    public Integer getRuleLateThresholdMinutes() {
        return ruleLateThresholdMinutes;
    }

    public void setRuleLateThresholdMinutes(Integer ruleLateThresholdMinutes) {
        this.ruleLateThresholdMinutes = ruleLateThresholdMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
