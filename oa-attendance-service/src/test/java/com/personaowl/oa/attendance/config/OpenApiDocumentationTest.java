package com.personaowl.oa.attendance.config;

import com.personaowl.oa.attendance.api.AttendanceController;
import com.personaowl.oa.attendance.api.AttendanceStatisticsController;
import com.personaowl.oa.attendance.api.AttendanceStatusController;
import com.personaowl.oa.common.core.web.RequestHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiDocumentationTest {

    private static final List<Class<?>> API_CONTROLLERS = List.of(
            AttendanceController.class,
            AttendanceStatisticsController.class,
            AttendanceStatusController.class);

    @Test
    void publishesAttendanceApiMetadataAndJwtSecurityScheme() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.attendanceOpenApi();
        SecurityScheme securityScheme = OpenApiConfig.class.getAnnotation(SecurityScheme.class);

        assertEquals("OA 考勤服务 API", openAPI.getInfo().getTitle());
        assertEquals("1.0", openAPI.getInfo().getVersion());
        assertNotNull(securityScheme);
        assertEquals("bearerAuth", securityScheme.name());
        assertEquals("bearer", securityScheme.scheme());
        assertEquals("JWT", securityScheme.bearerFormat());
    }

    @Test
    void documentsEveryControllerAndEndpoint() {
        for (Class<?> controller : API_CONTROLLERS) {
            assertNotNull(controller.getAnnotation(Tag.class), controller.getSimpleName() + " 缺少 @Tag");

            for (Method method : controller.getDeclaredMethods()) {
                if (isHttpEndpoint(method)) {
                    Operation operation = method.getAnnotation(Operation.class);
                    assertNotNull(operation, method + " 缺少 @Operation");
                    assertFalse(operation.summary().isBlank(), method + " 缺少接口摘要");
                    assertTrue(Arrays.stream(operation.security())
                                    .map(SecurityRequirement::name)
                                    .anyMatch("bearerAuth"::equals),
                            method + " 缺少 JWT 安全要求");
                }
            }
        }
    }

    @Test
    void hidesGatewayTrustedHeadersFromPublicDocumentation() {
        for (Class<?> controller : API_CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                java.lang.reflect.Parameter[] parameters = method.getParameters();
                Type[] parameterTypes = method.getGenericParameterTypes();
                for (int index = 0; index < parameters.length; index++) {
                    RequestHeader requestHeader = parameters[index].getAnnotation(RequestHeader.class);
                    if (requestHeader == null || !isTrustedHeader(requestHeader.value())) {
                        continue;
                    }

                    Parameter apiParameter = parameters[index].getAnnotation(Parameter.class);
                    assertNotNull(apiParameter,
                            method + " 的可信请求头缺少 @Parameter: " + parameterTypes[index]);
                    assertTrue(apiParameter.hidden(),
                            method + " 暴露了内部可信请求头 " + requestHeader.value());
                }
            }
        }
    }

    @Test
    void marksCompatibilityStatusEndpointDeprecated() throws NoSuchMethodException {
        Method status = AttendanceStatusController.class.getDeclaredMethod("status", String.class);
        Operation operation = status.getAnnotation(Operation.class);

        assertTrue(status.isAnnotationPresent(Deprecated.class));
        assertNotNull(operation);
        assertTrue(operation.deprecated());
    }

    private boolean isHttpEndpoint(Method method) {
        return method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class);
    }

    private boolean isTrustedHeader(String header) {
        return RequestHeaders.USER_ID.equals(header)
                || RequestHeaders.PERMISSIONS.equals(header)
                || RequestHeaders.TRACE_ID.equals(header);
    }
}
