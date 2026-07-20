package com.personaowl.oa.common.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {
    @Test
    void createsSuccessfulResponse() {
        ApiResponse<String> response = ApiResponse.success("ok", "trace-1");

        assertEquals("0", response.code());
        assertEquals("ok", response.data());
        assertEquals("trace-1", response.traceId());
    }

    @Test
    void createsFailureResponseWithoutData() {
        ApiResponse<Void> response = ApiResponse.failure("B0101", "bad request", "trace-2");

        assertEquals("B0101", response.code());
        assertNull(response.data());
    }
}

