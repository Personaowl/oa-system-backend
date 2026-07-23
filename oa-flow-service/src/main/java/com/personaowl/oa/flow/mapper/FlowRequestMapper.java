package com.personaowl.oa.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.flow.domain.entity.FlowRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FlowRequestMapper extends BaseMapper<FlowRequest> {
    @Select("""
            SELECT * FROM flow_request
            WHERE applicant_id = #{applicantId}
            ORDER BY created_at DESC, id DESC
            """)
    List<FlowRequest> findMine(@Param("applicantId") Long applicantId);

    @Select("""
            SELECT * FROM flow_request
            WHERE current_approver_id = #{approverId} AND status = 'PENDING'
            ORDER BY created_at ASC, id ASC
            """)
    List<FlowRequest> findTodo(@Param("approverId") Long approverId);

    @Select("""
            SELECT r.* FROM flow_request r
            JOIN flow_action_log l ON l.request_id = r.id
            WHERE l.operator_id = #{approverId}
            ORDER BY l.operated_at DESC, l.id DESC
            """)
    List<FlowRequest> findDone(@Param("approverId") Long approverId);

    @Update("""
            UPDATE flow_request
            SET status = #{newStatus}, current_approver_id = NULL, updated_at = #{updatedAt}
            WHERE id = #{requestId} AND status = 'PENDING'
              AND current_approver_id = #{approverId}
            """)
    int completeApproval(@Param("requestId") Long requestId,
                         @Param("approverId") Long approverId,
                         @Param("newStatus") String newStatus,
                         @Param("updatedAt") LocalDateTime updatedAt);
}
