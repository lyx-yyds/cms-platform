package com.cms.service.impl;

import com.cms.entity.VisitLog;
import com.cms.mapper.VisitLogMapper;
import com.cms.service.VisitLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 访问日志服务实现（异步落库，避免写库阻塞业务请求）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitLogServiceImpl implements VisitLogService {

    private final VisitLogMapper visitLogMapper;

    @Override
    @Async("visitLogExecutor")
    public void recordAsync(Long userId, String ip, String userAgent, String url, String method, int responseTime) {
        try {
            VisitLog log = new VisitLog();
            log.setUserId(userId);
            log.setIpAddress(ip);
            log.setUserAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent);
            log.setRequestUrl(url != null && url.length() > 500 ? url.substring(0, 500) : url);
            log.setRequestMethod(method);
            log.setResponseTime(responseTime);
            visitLogMapper.insert(log);
        } catch (Exception e) {
            // 访问日志失败不影响业务
            log.debug("访问日志写入失败: {}", e.getMessage());
        }
    }
}
