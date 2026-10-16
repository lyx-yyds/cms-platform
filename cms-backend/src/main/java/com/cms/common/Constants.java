package com.cms.common;

/**
 * 常量定义
 */
public final class Constants {

    private Constants() {
    }

    /** RefreshToken Redis Key 前缀：refresh:token:{jti}（文档 7.1） */
    public static final String REDIS_KEY_REFRESH_TOKEN = "refresh:token:";

    /** 密码重置令牌 Redis Key 前缀：reset:token:{token}（文档 7.1，TTL 30 分钟） */
    public static final String REDIS_KEY_RESET_TOKEN = "reset:token:";

    /** 密码重置令牌有效期（毫秒，30 分钟） */
    public static final long RESET_TOKEN_TTL_MS = 30 * 60 * 1000L;

    /** 浏览量计数 Key：article:views:{id}（文档 7.7） */
    public static final String REDIS_KEY_ARTICLE_VIEWS = "article:views:";

    /** 点赞集合 Key：article:liked:{id}（文档 7.7） */
    public static final String REDIS_KEY_ARTICLE_LIKED = "article:liked:";

    /** 评论状态：待审核 / 已通过 / 已拒绝（文档 7.5） */
    public static final int COMMENT_STATUS_PENDING = 0;
    public static final int COMMENT_STATUS_PASSED = 1;
    public static final int COMMENT_STATUS_REJECTED = 2;

    /** AI 机审状态（文档 7.5） */
    public static final int AI_STATUS_NONE = 0;
    public static final int AI_STATUS_PASS = 1;
    public static final int AI_STATUS_REVIEW = 2;
    public static final int AI_STATUS_REJECT = 3;

    /** 文章状态 */
    public static final int ARTICLE_STATUS_DRAFT = 0;
    public static final int ARTICLE_STATUS_PUBLISHED = 1;
    public static final int ARTICLE_STATUS_OFFLINE = 2;

    private static final String ROLE_PREFIX = "ROLE_";

    public static String roleCode(String code) {
        return code.startsWith(ROLE_PREFIX) ? code : ROLE_PREFIX + code;
    }
}
