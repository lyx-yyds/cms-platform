package com.cms.job;

import com.cms.common.Constants;
import com.cms.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 浏览量异步落库任务（文档 7.7.1）：
 * 定时把 Redis 增量 article:views:{id} 批量 UPDATE 到 cms_article.view_count，
 * 应用启动时补偿一次，避免停机丢失。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountFlushJob {

    private final StringRedisTemplate stringRedisTemplate;
    private final ArticleMapper articleMapper;

    @Scheduled(fixedDelayString = "${job.view-count-flush-interval-ms:300000}")
    public void flush() {
        try {
            Set<String> keys = stringRedisTemplate.keys(Constants.REDIS_KEY_ARTICLE_VIEWS + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            int flushed = 0;
            for (String key : keys) {
                String deltaStr = stringRedisTemplate.opsForValue().getAndDelete(key);
                if (deltaStr == null || "0".equals(deltaStr)) {
                    continue;
                }
                long delta = Long.parseLong(deltaStr);
                long articleId = Long.parseLong(key.substring(Constants.REDIS_KEY_ARTICLE_VIEWS.length()));
                articleMapper.updateViewCount(articleId, delta);
                flushed++;
            }
            if (flushed > 0) {
                log.info("ViewCountFlushJob: flushed {} article view-count increments to DB", flushed);
            }
        } catch (Exception e) {
            // Redis 未就绪/瞬时异常不阻断主流程，下个周期重试
            log.warn("ViewCountFlushJob failed: {}", e.getMessage());
        }
    }

    /** 应用启动补偿（文档 7.7.1 第 4 条） */
    @EventListener(ApplicationReadyEvent.class)
    public void flushOnStartup() {
        flush();
    }
}
