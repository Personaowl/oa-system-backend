package com.personaowl.oa.attendance.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.personaowl.oa.attendance.domain.CalendarDayType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("attendance_calendar_day")
public class AttendanceCalendarDayEntity {
    @TableId
    private LocalDate workDate;
    private CalendarDayType dayType;
    private String holidayName;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public CalendarDayType getDayType() { return dayType; }
    public void setDayType(CalendarDayType dayType) { this.dayType = dayType; }
    public String getHolidayName() { return holidayName; }
    public void setHolidayName(String holidayName) { this.holidayName = holidayName; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
