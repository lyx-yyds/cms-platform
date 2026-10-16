-- ============================================================
-- CMS 内容管理系统 建表脚本（对齐 v2.1 文档 4.2，含 v2.0/v2.1 新增字段）
-- 执行方式：mysql -uroot -p < init.sql  （需先 CREATE DATABASE cms）
-- ============================================================

CREATE DATABASE IF NOT EXISTS `cms` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `cms`;

-- ---------- 4.2.1 用户表 ----------
CREATE TABLE `sys_user` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`     VARCHAR(50)     NOT NULL COMMENT '用户名',
  `password`     VARCHAR(100)    NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname`     VARCHAR(50)     DEFAULT NULL COMMENT '昵称',
  `email`        VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
  `avatar`       VARCHAR(500)    DEFAULT NULL COMMENT '头像URL',
  `phone`        VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
  `status`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1-正常 0-禁用',
  `last_login_time` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------- 4.2.2 角色表 ----------
CREATE TABLE `sys_role` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name`         VARCHAR(50)     NOT NULL COMMENT '角色名称',
  `code`         VARCHAR(50)     NOT NULL COMMENT '角色编码',
  `description`  VARCHAR(200)    DEFAULT NULL COMMENT '角色描述',
  `status`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1-正常 0-禁用',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ---------- 4.2.3 权限表 ----------
CREATE TABLE `sys_permission` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `name`         VARCHAR(50)     NOT NULL COMMENT '权限名称',
  `code`         VARCHAR(100)    NOT NULL COMMENT '权限编码',
  `type`         TINYINT         NOT NULL DEFAULT 1 COMMENT '1-菜单 2-按钮 3-接口',
  `parent_id`    BIGINT          DEFAULT 0 COMMENT '父权限ID',
  `path`         VARCHAR(200)    DEFAULT NULL COMMENT '路由路径',
  `icon`         VARCHAR(100)    DEFAULT NULL COMMENT '图标',
  `sort_order`   INT             DEFAULT 0 COMMENT '排序',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ---------- 4.2.4 用户角色关联表（v2.1 修正：多对多） ----------
CREATE TABLE `sys_user_role` (
  `user_id`      BIGINT NOT NULL COMMENT '用户ID',
  `role_id`      BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ---------- 4.2.5 角色权限关联表 ----------
CREATE TABLE `sys_role_permission` (
  `role_id`       BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ---------- 4.2.6 文章分类表 ----------
CREATE TABLE `cms_category` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`         VARCHAR(50)     NOT NULL COMMENT '分类名称',
  `slug`         VARCHAR(50)     DEFAULT NULL COMMENT '分类别名（URL友好）',
  `description`  VARCHAR(200)    DEFAULT NULL COMMENT '分类描述',
  `parent_id`    BIGINT          DEFAULT 0 COMMENT '父分类ID',
  `sort_order`   INT             DEFAULT 0 COMMENT '排序',
  `status`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1-正常 0-禁用',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类表';

-- ---------- 4.2.7 标签表 ----------
CREATE TABLE `cms_tag` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name`         VARCHAR(50)     NOT NULL COMMENT '标签名称',
  `slug`         VARCHAR(50)     DEFAULT NULL COMMENT '标签别名',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  UNIQUE KEY `uk_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- ---------- 4.2.8 文章表（v2.1：is_deleted 逻辑删除） ----------
CREATE TABLE `cms_article` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title`          VARCHAR(200)    NOT NULL COMMENT '文章标题',
  `content`        LONGTEXT        COMMENT '文章内容（HTML）',
  `summary`        VARCHAR(500)    DEFAULT NULL COMMENT '文章摘要',
  `cover_image`    VARCHAR(500)    DEFAULT NULL COMMENT '封面图URL',
  `category_id`    BIGINT          DEFAULT NULL COMMENT '分类ID',
  `author_id`      BIGINT          NOT NULL COMMENT '作者ID',
  `status`         TINYINT         NOT NULL DEFAULT 0 COMMENT '0-草稿 1-已发布 2-已下架',
  `is_top`         BOOLEAN         DEFAULT FALSE COMMENT '是否置顶',
  `is_recommend`   BOOLEAN         DEFAULT FALSE COMMENT '是否推荐',
  `view_count`     INT             DEFAULT 0 COMMENT '浏览量（Redis异步落库）',
  `like_count`     INT             DEFAULT 0 COMMENT '点赞数',
  `comment_count`  INT             DEFAULT 0 COMMENT '评论数',
  `seo_title`      VARCHAR(200)    DEFAULT NULL COMMENT 'SEO标题',
  `seo_description` VARCHAR(500)   DEFAULT NULL COMMENT 'SEO描述',
  `seo_keywords`   VARCHAR(200)    DEFAULT NULL COMMENT 'SEO关键词',
  `publish_time`   DATETIME        DEFAULT NULL COMMENT '发布时间',
  `is_deleted`     BOOLEAN         DEFAULT FALSE COMMENT '逻辑删除标记（v2.1，见7.10）',
  `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- ---------- 4.2.9 文章标签关联表 ----------
CREATE TABLE `cms_article_tag` (
  `article_id`   BIGINT NOT NULL COMMENT '文章ID',
  `tag_id`       BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`article_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

-- ---------- 4.2.10 评论表（v2.0：ai_status 机审状态） ----------
CREATE TABLE `cms_comment` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id`   BIGINT          NOT NULL COMMENT '文章ID',
  `user_id`      BIGINT          NOT NULL COMMENT '评论用户ID',
  `parent_id`    BIGINT          DEFAULT 0 COMMENT '父评论ID（0表示顶级评论）',
  `reply_user_id` BIGINT         DEFAULT NULL COMMENT '回复用户ID',
  `content`      TEXT            NOT NULL COMMENT '评论内容',
  `status`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已拒绝',
  `ai_status`    TINYINT         NOT NULL DEFAULT 0 COMMENT 'AI机审状态：0-未机审 1-机审通过 2-转人工 3-机审拒绝',
  `ip_address`   VARCHAR(45)     DEFAULT NULL COMMENT 'IP地址',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ---------- 4.2.11 媒体文件表 ----------
CREATE TABLE `cms_media` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '媒体ID',
  `name`         VARCHAR(200)    NOT NULL COMMENT '文件名（服务端生成）',
  `original_name` VARCHAR(200)   NOT NULL COMMENT '原始文件名',
  `url`          VARCHAR(500)    NOT NULL COMMENT '文件URL',
  `type`         VARCHAR(50)     DEFAULT NULL COMMENT '文件类型（MIME）',
  `size`         BIGINT          DEFAULT 0 COMMENT '文件大小（字节）',
  `extension`    VARCHAR(20)     DEFAULT NULL COMMENT '文件扩展名',
  `upload_user_id` BIGINT        NOT NULL COMMENT '上传用户ID',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_upload_user_id` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='媒体文件表';

-- ---------- 4.2.12 访问日志表 ----------
CREATE TABLE `sys_visit_log` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`      BIGINT          DEFAULT NULL COMMENT '用户ID',
  `ip_address`   VARCHAR(45)     DEFAULT NULL COMMENT 'IP地址',
  `user_agent`   VARCHAR(500)    DEFAULT NULL COMMENT '浏览器UA',
  `request_url`  VARCHAR(500)    DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10)   DEFAULT NULL COMMENT '请求方法',
  `response_time` INT            DEFAULT NULL COMMENT '响应时间（ms）',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问日志表';

-- ---------- 4.2.13 AI 生成记录表（v2.0） ----------
CREATE TABLE `ai_generation_record` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`      BIGINT          NOT NULL COMMENT '用户ID',
  `article_id`   BIGINT          DEFAULT NULL COMMENT '关联文章ID',
  `type`         VARCHAR(30)     NOT NULL COMMENT '生成类型：draft草稿/continue续写/polish润色/summary摘要/seo',
  `prompt`       TEXT            COMMENT '请求提示词',
  `result`       LONGTEXT        COMMENT '生成结果',
  `model`        VARCHAR(50)     DEFAULT NULL COMMENT '使用的模型',
  `status`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0-处理中 1-成功 2-失败',
  `tokens_used`  INT             DEFAULT 0 COMMENT '消耗Token数（成本审计）',
  `error_msg`    VARCHAR(500)    DEFAULT NULL COMMENT '失败原因',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI生成记录表';

-- ---------- 4.2.14 AI 评论审核记录表（v2.0） ----------
CREATE TABLE `ai_review_record` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `comment_id`    BIGINT          NOT NULL COMMENT '评论ID',
  `content`       TEXT            COMMENT '评论内容快照',
  `review_result` VARCHAR(20)     NOT NULL COMMENT '机审结果：normal/ad/repeat/abuse/sensitive',
  `score`         DECIMAL(5,2)    DEFAULT NULL COMMENT '违规置信度',
  `model`         VARCHAR(50)     DEFAULT NULL COMMENT '模型',
  `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1-机审完成',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI评论审核记录表';

-- ---------- 4.2.15 文章向量分块表（P1 可选） ----------
CREATE TABLE `ai_article_chunk` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分块ID',
  `article_id`   BIGINT          NOT NULL COMMENT '文章ID',
  `chunk_index`  INT             NOT NULL COMMENT '分块序号',
  `content`      TEXT            NOT NULL COMMENT '分块内容',
  `token_count`  INT             DEFAULT 0 COMMENT 'Token数',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章向量分块表（元数据，向量存于Milvus）';

-- ============================================================
-- 初始数据
-- ============================================================

-- 角色
INSERT INTO `sys_role` (`id`, `name`, `code`, `description`) VALUES
  (1, '超级管理员', 'ADMIN', '拥有所有权限'),
  (2, '编辑',      'EDITOR', '文章/分类/标签/评论/AI 管理'),
  (3, '作者',      'AUTHOR', '文章发布与编辑（自己的）'),
  (4, '普通用户',  'USER', '评论发布、文章浏览');

-- 权限（type: 1-菜单 2-按钮 3-接口）
INSERT INTO `sys_permission` (`id`, `name`, `code`, `type`) VALUES
  (1,  '文章列表',   'article:list',   3),
  (2,  '创建文章',   'article:create', 3),
  (3,  '修改文章',   'article:update', 3),
  (4,  '删除文章',   'article:delete', 3),
  (5,  '评论审核',   'comment:audit',  3),
  (6,  '用户管理',   'user:manage',    3),
  (7,  '媒体上传',   'media:upload',   3),
  (8,  'AI 生成',    'ai:use',         3),
  (9,  '数据统计',   'stats:view',     3);

-- 角色权限：ADMIN 拥有全部
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
  (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
  -- EDITOR：文章管理 + 评论审核 + AI + 统计
  (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 8), (2, 9),
  -- AUTHOR：文章 + AI
  (3, 1), (3, 2), (3, 3), (3, 8),
  -- USER：浏览
  (4, 1);

-- 管理员账号：admin / admin123（BCryptPasswordEncoder 生成，验证通过）
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`) VALUES
  (1, 'admin', '$2a$10$AikHTyDhvQtaZIWqcCfrl.T0v41WusrgIT3sYwu3Ip8xc6ta06/qi', '主编', 'admin@cms.local', 1);

-- 用户角色：admin → 超级管理员
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);
