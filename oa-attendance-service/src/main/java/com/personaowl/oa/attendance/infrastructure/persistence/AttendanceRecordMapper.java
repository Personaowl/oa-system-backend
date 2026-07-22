package com.personaowl.oa.attendance.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.attendance.domain.AttendanceStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
                         @Param("version") int version);

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
