package com.personaowl.oa.attendance.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface AttendanceShiftAssignmentMapper extends BaseMapper<AttendanceShiftAssignmentEntity> {
    @Select("""
            SELECT COUNT(*)
            FROM attendance_shift_assignment
            WHERE user_id = #{userId}
              AND start_date <= #{endDate}
              AND end_date >= #{startDate}
            """)
    long countOverlapping(@Param("userId") long userId,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);
}
