package com.personaowl.oa.attendance.support;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperatorContextTest {

    @Test
    void parsesTrustedIdentityAndPermissions() {
        OperatorContext context = OperatorContext.fromHeaders(
                " 10001 ", "attendance:record:query, notice:read,attendance:record:query", "trace-1");

        assertEquals(10001L, context.userId());
        assertEquals(Set.of("attendance:record:query", "notice:read"), context.permissions());
        assertEquals("trace-1", context.traceId());
    }

    @Test
    void rejectsMissingOrInvalidTrustedIdentity() {
        BusinessException missing = assertThrows(BusinessException.class,
                () -> OperatorContext.fromHeaders(null, null, null));
        BusinessException invalid = assertThrows(BusinessException.class,
                () -> OperatorContext.fromHeaders("not-a-number", null, null));
        BusinessException nonPositive = assertThrows(BusinessException.class,
                () -> OperatorContext.fromHeaders("0", null, null));

        assertEquals(ErrorCode.UNAUTHORIZED, missing.errorCode());
        assertEquals(ErrorCode.UNAUTHORIZED, invalid.errorCode());
        assertEquals(ErrorCode.UNAUTHORIZED, nonPositive.errorCode());
    }
}
