package com.cms.interceptor;

import com.cms.security.SecurityUtils;
import com.cms.service.VisitLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 访问日志拦截器（interceptor/VisitLogInterceptor.java，文档 7.11）
 * 记录 /api/** 请求的 user/ip/UA/url/method/响应时间，异步落库
 */
@Component
@RequiredArgsConstructor
public class VisitLogInterceptor implements HandlerInterceptor {

    private static final String ATTR_START = "visitLogStart";

    private final VisitLogService visitLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        request.setAttribute(ATTR_START, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute(ATTR_START);
        if (start == null) {
            return;
        }
        long cost = System.currentTimeMillis() - (Long) start;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        Long userId = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception ignored) {
            // 未登录请求
        }
        visitLogService.recordAsync(userId, ip, request.getHeader("User-Agent"),
                request.getRequestURI(), request.getMethod(), (int) Math.min(cost, 99999));
    }
}
