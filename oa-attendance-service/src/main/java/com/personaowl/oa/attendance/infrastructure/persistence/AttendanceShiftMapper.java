package com.personaowl.oa.attendance.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface AttendanceShiftMapper extends BaseMapper<AttendanceShiftEntity> {
    @Select("""
            SELECT s.*
            FROM attendance_shift_assignment a
            JOIN attendance_shift s ON s.id = a.shift_id
            WHERE a.user_id = #{userId}
              AND #{workDate} BETWEEN a.start_date AND a.end_date
              AND s.status = 1
            ORDER BY a.start_date DESC, a.id DESC
            LIMIT 1
            """)
    AttendanceShiftEntity findAssignedShift(@Param("userId") long userId,
                                            @Param("workDate") LocalDate workDate);
}
