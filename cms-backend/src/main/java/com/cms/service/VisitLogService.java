package com.cms.service;

/**
 * 访问日志服务（文档 4.2.12 / 7.11）
 */
public interface VisitLogService {

    /** 异步记录访问日志 */
    void recordAsync(Long userId, String ip, String userAgent, String url, String method, int responseTime);
}
