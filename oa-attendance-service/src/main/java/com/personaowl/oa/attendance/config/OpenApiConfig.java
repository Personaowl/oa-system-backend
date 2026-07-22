package com.personaowl.oa.attendance.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "通过 oa-gateway 登录后取得的访问令牌")
public class OpenApiConfig {

    @Bean
    public OpenAPI attendanceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("OA 考勤服务 API")
                .description("上下班打卡、考勤记录、个人月度统计与管理员汇总接口")
                .version("1.0"));
    }
}
