package com.personaowl.oa.attendance.support;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class AttendanceAuthorizationService {

    public static final String RECORD_QUERY_PERMISSION = "attendance:record:query";
    public static final String STATISTICS_QUERY_PERMISSION = "attendance:statistics:query";

    public RecordQueryScope resolveRecordQueryScope(OperatorContext operator,
                                                    Long requestedUserId,
                                                    Long departmentId) {
        boolean canQueryAll = operator.permissions().contains(RECORD_QUERY_PERMISSION);
        if (!canQueryAll) {
            if (departmentId != null
                    || (requestedUserId != null && requestedUserId != operator.userId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return new RecordQueryScope(
                    operator.userId(),
                    "SELF",
                    false,
                    "仅查询当前用户本人记录");
        }

        String dataScope = requestedUserId == null ? "ALL_USERS" : "SPECIFIC_USER";
        String scopeNote = departmentId == null
                ? (requestedUserId == null ? "查询全部用户记录" : "按指定用户查询记录")
                : "departmentId 本期仅预留，未参与数据过滤";
        return new RecordQueryScope(requestedUserId, dataScope, false, scopeNote);
    }

    public record RecordQueryScope(
            Long targetUserId,
            String dataScope,
            boolean departmentFilterApplied,
            String scopeNote
    ) {
    }

    public void requireStatisticsQuery(OperatorContext operator) {
        if (!operator.permissions().contains(STATISTICS_QUERY_PERMISSION)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
