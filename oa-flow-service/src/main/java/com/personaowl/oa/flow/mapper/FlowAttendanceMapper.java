package com.personaowl.oa.flow.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface FlowAttendanceMapper {
    @Insert("""
            INSERT IGNORE INTO attendance_record (
                id, user_id, work_date, check_in_time, check_out_time, status,
                late_minutes, early_leave_minutes, rule_work_start, rule_work_end,
                rule_late_threshold_minutes, created_at, updated_at, version
            ) VALUES (
                #{id}, #{userId}, #{workDate}, NULL, NULL, 'LEAVE',
                0, 0, NULL, NULL, NULL, #{createdAt}, #{createdAt}, 0
            )
            """)
    int insertApprovedLeave(@Param("id") Long id,
                            @Param("userId") Long userId,
                            @Param("workDate") LocalDate workDate,
                            @Param("createdAt") LocalDateTime createdAt);
}
