package com.cms.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码（对应前端 request.ts 解包逻辑：code=200 成功）
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    // 业务
    USERNAME_EXISTS(1001, "用户名已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "用户名或密码错误"),
    PASSWORD_MISMATCH(1004, "原密码不正确"),
    USER_DISABLED(1004, "账号已被禁用"),
    REFRESH_TOKEN_INVALID(1005, "RefreshToken 无效或已使用"),
    RESET_TOKEN_INVALID(1006, "重置令牌无效或已过期"),
    AI_GENERATION_FAILED(2001, "AI 生成失败"),
    AI_RATE_LIMITED(2002, "AI 调用过于频繁");

    private final int code;
    private final String message;
}
