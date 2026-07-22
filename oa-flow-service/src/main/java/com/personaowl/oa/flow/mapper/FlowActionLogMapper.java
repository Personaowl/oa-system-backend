package com.personaowl.oa.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.flow.domain.entity.FlowActionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FlowActionLogMapper extends BaseMapper<FlowActionLog> {
    @Select("""
            SELECT * FROM flow_action_log
            WHERE request_id = #{requestId}
            ORDER BY operated_at DESC, id DESC LIMIT 1
            """)
    FlowActionLog findLatest(@Param("requestId") Long requestId);

    @Select("""
            SELECT COUNT(1) FROM flow_action_log
            WHERE request_id = #{requestId} AND operator_id = #{operatorId}
            """)
    long countByRequestAndOperator(@Param("requestId") Long requestId,
                                   @Param("operatorId") Long operatorId);
}
