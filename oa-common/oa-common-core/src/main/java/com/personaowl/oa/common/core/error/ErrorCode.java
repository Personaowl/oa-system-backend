package com.personaowl.oa.common.core.error;

public enum ErrorCode {
    INVALID_CREDENTIALS("A0101", "用户名或密码错误"),
    TOKEN_EXPIRED("A0102", "登录状态已过期"),
    UNAUTHORIZED("A0103", "请先登录"),
    FORBIDDEN("A0201", "无权执行该操作"),
    INVALID_ARGUMENT("B0101", "请求参数不正确"),
    BUSINESS_RULE_VIOLATION("B0102", "业务规则校验失败"),
    ATTENDANCE_ALREADY_CHECKED_IN("B0110", "今日已完成上班打卡"),
    ATTENDANCE_ALREADY_CHECKED_OUT("B0111", "今日已完成下班打卡"),
    ATTENDANCE_CHECK_IN_REQUIRED("B0112", "请先完成上班打卡"),
    ATTENDANCE_REQUEST_IN_PROGRESS("B0114", "考勤请求处理中，请稍后重试"),
    REDIS_UNAVAILABLE("C0102", "缓存服务不可用"),
    SYSTEM_ERROR("S0001", "系统繁忙，请稍后重试");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
