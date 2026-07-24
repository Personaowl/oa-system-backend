package com.personaowl.oa.attendance.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.attendance.domain.AttendanceStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecordEntity> {

    @Update("""
            UPDATE attendance_record
            SET check_out_time = #{checkOutTime},
                status = #{status},
                early_leave_minutes = #{earlyLeaveMinutes},
                actual_work_minutes = #{actualWorkMinutes},
                updated_at = #{checkOutTime},
                version = version + 1
            WHERE id = #{id}
              AND check_out_time IS NULL
              AND version = #{version}
            """)
    int completeCheckOut(@Param("id") long id,
                         @Param("checkOutTime") LocalDateTime checkOutTime,
                         @Param("status") AttendanceStatus status,
                         @Param("earlyLeaveMinutes") int earlyLeaveMinutes,
                         @Param("actualWorkMinutes") int actualWorkMinutes,
                         @Param("version") int version);

    @Update("""
            UPDATE attendance_record
            SET status = 'MISSING_CHECK_OUT',
                updated_at = #{updatedAt},
                version = version + 1
            WHERE work_date = #{workDate}
              AND check_in_time IS NOT NULL
              AND check_out_time IS NULL
              AND status IN ('IN_PROGRESS', 'IN_PROGRESS_LATE')
            """)
    int finalizeMissingCheckOut(@Param("workDate") LocalDate workDate,
                                @Param("updatedAt") LocalDateTime updatedAt);

    @Select("""
            SELECT u.id
            FROM sys_user u
            JOIN sys_department d ON d.id = u.department_id
            WHERE u.status = 1 AND u.deleted = 0
              AND d.status = 1 AND d.deleted = 0
            ORDER BY u.id
            """)
    List<Long> findActiveAttendanceUserIds();

    @Insert("""
            INSERT IGNORE INTO attendance_record
            (id, user_id, work_date, status, late_minutes, early_leave_minutes,
             created_at, updated_at, version)
            VALUES
            (#{id}, #{userId}, #{workDate}, 'ABSENT', 0, 0, #{now}, #{now}, 0)
            """)
    int insertAbsentIfMissing(@Param("id") Long id,
                              @Param("userId") Long userId,
                              @Param("workDate") LocalDate workDate,
                              @Param("now") LocalDateTime now);

    AttendanceStatisticsAggregate aggregateStatistics(@Param("userId") Long userId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate,
                                                       @Param("today") LocalDate today,
                                                       @Param("departmentIds") List<Long> departmentIds);

    default AttendanceStatisticsAggregate aggregateStatistics(Long userId,
                                                               LocalDate startDate,
                                                               LocalDate endDate,
                                                               LocalDate today) {
        return aggregateStatistics(userId, startDate, endDate, today, null);
    }
}
