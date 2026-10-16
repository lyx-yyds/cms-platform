# CMS内容管理系统 - 项目设计文档（需求文档 v2.0）

> 项目名称：cms-system
> 文档版本：v2.1
> 创建日期：2026-06-22
> 更新日期：2026-09-29
> 更新说明：v2.0 基于评审结论修订——① 修正认证/缓存设计自相矛盾之处；② 新增 Spring AI 智能能力（写作助手、评论 AI 初审、站内问答）；③ 新增并发与安全设计；④ 性能指标改为"目标 + 验证方式"可审计写法
> 更新说明：v2.1 基于文档评审结论修订——修正 ER 图与用户角色关联表冲突；补齐点赞/取消点赞接口；新增数据删除策略（7.10）与统计口径定义（7.11）；补充 nginx.conf 示例（9.4）与 MinIO bucket 初始化；加入核心接口测试计划与密码重置功能；修复目录树笔误；完善 RefreshToken 并发刷新兜底

---

## 一、项目概述

### 1.1 项目背景

内容管理系统（Content Management System）是企业和个人管理网站内容的核心工具。本项目旨在开发一个功能完整的 CMS 系统，涵盖文章管理、用户权限、数据统计、AI 智能助手等核心功能。

### 1.2 项目目标

- 实现文章发布、编辑、删除、分类、标签等核心功能
- 支持RBAC权限控制，实现多角色管理
- 提供数据统计和可视化功能
- 支持媒体文件上传和管理
- 集成 Spring AI，提供文章智能生成、评论智能审核能力
- 具备良好的扩展性、可维护性和安全性

### 1.3 核心功能清单

| 模块 | 功能 |
|------|------|
| 文章管理 | 文章发布、编辑、删除、分类、标签、草稿箱、富文本编辑 |
| 用户管理 | 用户列表、角色管理、权限控制 |
| 评论系统 | 评论发布、评论审核（AI 初审 + 人工复核）、评论管理 |
| 数据统计 | 访问量统计、用户统计、文章统计 |
| 媒体管理 | 图片上传、文件管理、媒体库 |
| AI 智能助手 | 文章草稿生成、续写润色、摘要与 SEO 自动生成、评论 AI 初审、站内智能问答 |
| 系统设置 | 网站配置、邮件配置（密码重置邮件）、SEO设置 |

---

## 二、技术栈选型

### 2.1 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | 前端框架 |
| TypeScript | 5.x | 类型系统 |
| Element Plus | 2.x | UI组件库 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.x | HTTP请求 |
| ECharts | 5.x | 数据可视化 |
| TinyMCE | 6.x | 富文本编辑器 |
| Vite | 5.x | 构建工具 |

### 2.2 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.4.x | 后端框架（Spring AI 1.x 要求 3.4+） |
| Spring AI | 1.1.x | AI 能力框架（当前稳定线；1.0.x 为长期维护分支） |
| MyBatis-Plus | 3.x | ORM框架 |
| MySQL | 8.0 | 数据库 |
| Redis | 7.x | 缓存 |
| MinIO | 最新 | 对象存储 |
| Spring Security | 6.x | 安全框架 |
| JWT | - | 身份认证（Access Token + Refresh Token 双令牌） |
| Spring Mail | 3.4.x | 邮件发送（密码重置邮件） |
| Knife4j | 4.x | API文档 |

### 2.3 AI 模型选型

| 方案 | 说明 | 适用场景 |
|------|------|----------|
| DashScope（通义千问 qwen-plus） | 阿里云模型服务，国内直连，按量计费 | 推荐主选，开发/生产均可 |
| Ollama（本地部署） | 本地推理，免费、无需 API Key | 面试演示、无外网环境 |
| OpenAI | 国际主流，需海外访问 | 备选，不做主方案 |

> 选型原则：国内网络环境优先 DashScope；演示环境可用 Ollama 兜底。模型相关代码通过 Spring AI 抽象（ChatClient）封装，切换模型仅需改配置。

### 2.4 开发工具

| 工具 | 说明 |
|------|------|
| IDEA | Java开发IDE |
| VS Code | 前端开发IDE |
| Navicat | 数据库管理 |
| Git | 版本控制 |
| Maven | 项目构建 |
| Docker | 容器化部署 |
| JMeter | 性能压测（验证性能指标） |

---

## 三、系统架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        用户浏览器                            │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                        Nginx                                │
│                  (反向代理 + 负载均衡)                        │
└──────────────────────────┬──────────────────────────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
┌────────▼────────┐ ┌──────▼──────┐ ┌───────▼───────┐
│   Vue3前端应用   │ │  API网关    │ │  静态资源服务  │
│   (前端渲染)    │ │  (Spring    │ │  (Nginx/MinIO) │
│                │ │   Boot)     │ │               │
└────────────────┘ └──────┬──────┘ └───────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
  ┌─────▼─────┐   ┌──────▼──────┐   ┌──────▼──────┐
  │   MySQL   │   │    Redis    │   │    MinIO    │
  │  (数据存储) │   │  (缓存层)   │   │ (文件存储)  │
  └───────────┘   └─────────────┘   └─────────────┘
                          │
        ┌─────────────────┴─────────────────┐
        │                                   │
  ┌─────▼──────────────┐        ┌──────────▼─────────┐
  │  Spring AI 服务层    │        │  向量库（P1 可选）    │
  │ ChatClient/Prompt   │        │  Milvus            │
  │ 模板/流式输出        │        │  (RAG 向量存储)      │
  └─────┬──────────────┘        └────────────────────┘
        │
  ┌─────▼─────────────┐
  │  模型服务          │
  │ DashScope / Ollama│
  └───────────────────┘
```

### 3.2 请求处理链路

```
前端请求：
  浏览器 → Nginx → Vue3应用 → API请求 → Spring Boot → MySQL/Redis

文件上传：
  浏览器 → Nginx → Spring Boot → MinIO → 返回文件URL

用户认证：
  登录请求 → Spring Boot → 验证密码 → 生成 AccessToken + RefreshToken → 返回
  后续请求 → 携带 AccessToken → JWT 过滤器验证 → 处理请求
  Token 过期 → 携带 RefreshToken → POST /api/auth/refresh → 返回新 AccessToken

AI 生成（写作助手）：
  前端 → Spring Boot → ChatClient 调用模型服务 → SSE 流式返回 → 前端逐字渲染
  → 生成结果落库（ai_generation_record）→ 可手动编辑后保存为正式文章

AI 审核（评论初审）：
  发表评论 → Spring Boot 异步调用模型分类 → 机审通过自动发布 / 疑似转人工
```

### 3.3 AI 能力架构

```
┌─────────────────────────────────────────────────────┐
│                 AI 能力层（Spring AI）                │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ │
│  │ AI 写作助手   │ │ 评论 AI 初审  │ │ 站内问答 RAG  │ │
│  │ 草稿/续写/    │ │ 分类审核      │ │ 向量检索+生成  │ │
│  │ 润色/摘要SEO │ │ 状态流转      │ │ (P1)         │ │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ │
│         │                │                │          │
│  ┌──────▼───────┐  ┌─────▼──────┐  ┌──────▼───────┐  │
│  │ ChatClient   │  │ ChatClient │  │ Embedding +  │  │
│  │ + 流式输出    │  │ + 结构化输出│  │ VectorStore  │  │
│  └──────┬───────┘  └─────┬──────┘  └──────┬───────┘  │
└─────────┼────────────────┼────────────────┼──────────┘
          ▼                ▼                ▼
    ┌─────────────────────────────────────────────┐
    │          模型服务（DashScope / Ollama）       │
    └─────────────────────────────────────────────┘

工程化配套：
  - Prompt 模板集中管理（resources/prompts/）
  - 异步化：线程池 + 降级策略（超时失败转人工/提示重试）
  - 成本控制：接口限流 + 生成记录审计
  - API Key 管理：环境变量注入，禁止硬编码
```

---

## 四、数据库设计

### 4.1 ER关系图

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────────┐
│    user      │       │     article      │       │    comment       │
├──────────────┤       ├──────────────────┤       ├──────────────────┤
│ id (PK)      │◄──┐   │ id (PK)          │   ┌──►│ id (PK)          │
│ username     │   └───│ author_id (FK)   │   │   │ article_id (FK)  │
│ password     │       │ category_id (FK) │───┘   │ user_id (FK)     │
│ email        │       │ title            │       │ content          │
│ avatar       │       │ content          │       │ parent_id        │
│ user_role:MN │       │ status           │       │ status           │
│ create_time  │       │ view_count       │       │ ai_status        │
└──────────────┘       │ create_time      │       └──────────────────┘
                       └──────────────────┘
                              │                      ┌──────────────────┐
                              │ 聚合                │  ai_review_record │
                              ▼                      ├──────────────────┤
                       ┌──────────────────┐         │ comment_id (FK)  │
                       │    category      │         │ review_result    │
                       ├──────────────────┤         │ score            │
                       │ id (PK)          │         └──────────────────┘
                       │ name             │
                       │ slug             │
                       └──────────────────┘
┌──────────────────┐
│ ai_generation_   │
│ record           │
├──────────────────┤
│ user_id (FK)     │
│ article_id (FK)  │
│ type / prompt    │
│ result / status  │
└──────────────────┘
```

> 说明（v2.1 修正）：user 与 role 为**多对多**关系，通过 `sys_user_role` 关联表实现（user 表本身无 role_id 字段）；role 与 permission 同理通过 `sys_role_permission` 关联。

### 4.2 建表SQL

#### 4.2.1 用户表

```sql
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
```

#### 4.2.2 角色表

```sql
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
```

#### 4.2.3 权限表

```sql
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
```

#### 4.2.4 用户角色关联表

```sql
CREATE TABLE `sys_user_role` (
  `user_id`      BIGINT NOT NULL COMMENT '用户ID',
  `role_id`      BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

#### 4.2.5 角色权限关联表

```sql
CREATE TABLE `sys_role_permission` (
  `role_id`       BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';
```

#### 4.2.6 文章分类表

```sql
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
```

#### 4.2.7 标签表

```sql
CREATE TABLE `cms_tag` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name`         VARCHAR(50)     NOT NULL COMMENT '标签名称',
  `slug`         VARCHAR(50)     DEFAULT NULL COMMENT '标签别名',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  UNIQUE KEY `uk_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';
```

#### 4.2.8 文章表

```sql
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
  `view_count`     INT             DEFAULT 0 COMMENT '浏览量（Redis异步落库，见7.7）',
  `like_count`     INT             DEFAULT 0 COMMENT '点赞数',
  `comment_count`  INT             DEFAULT 0 COMMENT '评论数',
  `seo_title`      VARCHAR(200)    DEFAULT NULL COMMENT 'SEO标题',
  `seo_description` VARCHAR(500)   DEFAULT NULL COMMENT 'SEO描述',
  `seo_keywords`   VARCHAR(200)    DEFAULT NULL COMMENT 'SEO关键词',
  `publish_time`   DATETIME        DEFAULT NULL COMMENT '发布时间',
  `is_deleted`     BOOLEAN         DEFAULT FALSE COMMENT '逻辑删除标记（v2.1 新增，见7.10）',
  `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';
```

#### 4.2.9 文章标签关联表

```sql
CREATE TABLE `cms_article_tag` (
  `article_id`   BIGINT NOT NULL COMMENT '文章ID',
  `tag_id`       BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`article_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';
```

#### 4.2.10 评论表

```sql
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
```

#### 4.2.11 媒体文件表

```sql
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
```

#### 4.2.12 访问日志表

```sql
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
```

#### 4.2.13 AI 生成记录表（v2.0 新增）

```sql
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
```

#### 4.2.14 AI 评论审核记录表（v2.0 新增）

```sql
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
```

#### 4.2.15 文章向量分块表（P1 可选）

```sql
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
```

### 4.3 索引设计说明

| 表 | 索引 | 类型 | 用途 |
|---|---|---|---|
| sys_user | uk_username | 唯一索引 | 保证用户名唯一 |
| sys_user | uk_email | 唯一索引 | 保证邮箱唯一 |
| cms_article | idx_category_id | 普通索引 | 按分类查询文章 |
| cms_article | idx_author_id | 普通索引 | 按作者查询文章 |
| cms_article | idx_status | 普通索引 | 按状态筛选文章 |
| cms_article | idx_publish_time | 普通索引 | 按发布时间排序 |
| cms_article | idx_is_deleted | 普通索引 | 逻辑删除过滤（v2.1） |
| cms_comment | idx_article_id | 普通索引 | 按文章查询评论 |
| cms_comment | idx_user_id | 普通索引 | 按用户查询评论 |
| ai_generation_record | idx_user_id | 普通索引 | 按用户查生成记录 |
| ai_generation_record | idx_article_id | 普通索引 | 按文章查生成记录 |
| ai_review_record | idx_comment_id | 普通索引 | 按评论查审核记录 |

---

## 五、核心接口设计

### 5.1 接口总览

#### 认证接口（v2.0 新增刷新）

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 用户登录 | POST | `/api/auth/login` | 否 | 返回 AccessToken + RefreshToken |
| 用户注册 | POST | `/api/auth/register` | 否 | 用户注册 |
| 刷新Token | POST | `/api/auth/refresh` | 否(带RefreshToken) | 用 RefreshToken 换新 AccessToken |
| 退出登录 | POST | `/api/auth/logout` | 是 | 注销 RefreshToken |
| 忘记密码 | POST | `/api/auth/forgot-password` | 否 | 发送密码重置邮件（v2.1 新增） |
| 重置密码 | POST | `/api/auth/reset-password` | 否 | 用邮件重置链接中的令牌重置密码（v2.1 新增） |

#### 用户接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 获取用户信息 | GET | `/api/user/info` | 是 | 返回当前用户信息 |
| 修改用户信息 | PUT | `/api/user/info` | 是 | 修改个人信息 |
| 用户列表 | GET | `/api/admin/users` | 是(ADMIN) | 分页查询用户 |
| 创建用户 | POST | `/api/admin/users` | 是(ADMIN) | 创建用户 |
| 修改用户 | PUT | `/api/admin/users/{id}` | 是(ADMIN) | 修改用户 |
| 删除用户 | DELETE | `/api/admin/users/{id}` | 是(ADMIN) | 删除用户 |

#### 文章接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 文章列表 | GET | `/api/articles` | 否 | 分页查询文章 |
| 文章详情 | GET | `/api/articles/{id}` | 否 | 返回文章详情 |
| 创建文章 | POST | `/api/articles` | 是 | 创建文章 |
| 修改文章 | PUT | `/api/articles/{id}` | 是 | 修改文章（仅作者或管理员） |
| 删除文章 | DELETE | `/api/articles/{id}` | 是 | 删除文章 |
| 文章点赞 | POST | `/api/articles/{id}/like` | 是 | 点赞文章（Redis Set 去重，SADD 幂等） |
| 取消点赞 | DELETE | `/api/articles/{id}/like` | 是 | 取消点赞（SREM 移除，v2.1 新增） |
| 我的文章 | GET | `/api/articles/mine` | 是 | 查询我的文章 |

#### 分类 / 标签接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 分类列表 | GET | `/api/categories` | 否 | 查询所有分类 |
| 创建分类 | POST | `/api/categories` | 是(ADMIN) | 创建分类 |
| 修改分类 | PUT | `/api/categories/{id}` | 是(ADMIN) | 修改分类 |
| 删除分类 | DELETE | `/api/categories/{id}` | 是(ADMIN) | 删除分类 |
| 标签列表 | GET | `/api/tags` | 否 | 查询所有标签 |
| 创建标签 | POST | `/api/tags` | 是 | 创建标签 |
| 删除标签 | DELETE | `/api/tags/{id}` | 是(ADMIN) | 删除标签 |

#### 评论接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 文章评论列表 | GET | `/api/articles/{id}/comments` | 否 | 查询文章评论（仅展示已通过） |
| 发表评论 | POST | `/api/articles/{id}/comments` | 是 | 发表评论（触发 AI 初审，见7.5） |
| 删除评论 | DELETE | `/api/comments/{id}` | 是 | 删除评论 |
| 审核评论 | PUT | `/api/admin/comments/{id}/audit` | 是(ADMIN) | 人工复核（覆盖机审结果） |
| 评论列表 | GET | `/api/admin/comments` | 是(ADMIN) | 查询所有评论（含机审状态） |

#### 媒体接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 上传文件 | POST | `/api/media/upload` | 是 | 上传文件（校验见7.8） |
| 文件列表 | GET | `/api/media` | 是 | 查询文件列表 |
| 删除文件 | DELETE | `/api/media/{id}` | 是 | 删除文件 |

#### 统计接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 访问统计 | GET | `/api/stats/visits` | 是(ADMIN) | 访问量统计 |
| 文章统计 | GET | `/api/stats/articles` | 是(ADMIN) | 文章统计 |
| 用户统计 | GET | `/api/stats/users` | 是(ADMIN) | 用户统计 |
| 仪表盘数据 | GET | `/api/stats/dashboard` | 是(ADMIN) | 仪表盘概览 |

#### AI 智能接口（v2.0 新增）

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 生成文章草稿 | POST | `/api/ai/write/draft` | 是 | 按标题+关键词生成草稿（SSE流式） |
| 续写文章 | POST | `/api/ai/write/continue` | 是 | 基于已有内容续写（SSE流式） |
| 润色文章 | POST | `/api/ai/write/polish` | 是 | 对内容润色改写（SSE流式） |
| 生成摘要与SEO | POST | `/api/ai/article/enhance` | 是 | 自动生成摘要 + SEO标题/描述/关键词 |
| 我的生成记录 | GET | `/api/ai/generations` | 是 | 分页查询当前用户生成记录 |
| 站内智能问答 | POST | `/api/ai/chat` | 是 | 基于站内文章 RAG 问答（P1 可选） |

> 说明：评论 AI 初审由发表评论接口内部异步触发，不单独暴露给前端。

### 5.2 接口示例

#### 用户登录（v2.0 新增 RefreshToken）

```json
// POST /api/auth/login

// Request
{
  "username": "admin",
  "password": "admin123"
}

// Response
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200,
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员",
      "avatar": "http://xxx.com/avatar.jpg",
      "roles": ["ADMIN"]
    }
  }
}
```

#### 刷新 Token

```json
// POST /api/auth/refresh

// Request（携带 RefreshToken）
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

// Response
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200
  }
}
```

#### AI 生成草稿（SSE 流式）

```text
// POST /api/ai/write/draft

// Request
{
  "title": "Vue3 组合式 API 入门指南",
  "keywords": "Vue3, Composition API, setup",
  "categoryId": 1,
  "outline": "1. 为什么用组合式 API 2. 核心用法 3. 与选项式对比"
}

// Response（text/event-stream，逐块返回）
data: {"content": "组合式 API 是 Vue3 的核心能力之一，它解决了..."}

// 最终（生成完成后前端合并渲染，用户可编辑后再保存）
```

#### 生成摘要与 SEO

```json
// POST /api/ai/article/enhance
// Request
{
  "articleId": 1,
  "content": "……完整文章内容……"
}

// Response
{
  "code": 200,
  "message": "success",
  "data": {
    "summary": "本文介绍 Vue3 组合式 API 的基本概念与核心用法……",
    "seoTitle": "Vue3 组合式 API 入门：setup 与响应式详解",
    "seoDescription": "从零掌握 Vue3 组合式 API，涵盖 setup 语法、ref/reactive 响应式与实战示例。",
    "seoKeywords": "Vue3,组合式API,setup,ref,reactive"
  }
}
```

#### 站内智能问答（P1）

```json
// POST /api/ai/chat
// Request
{
  "question": "系统如何配置邮件服务？"
}

// Response
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "根据站内文档《系统设置指南》，邮件配置位于……",
    "sources": [
      {"articleId": 12, "title": "系统设置指南", "score": 0.93}
    ]
  }
}
```

### 5.3 统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 5.4 错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 / Token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 429 | 请求过于频繁（触发限流） |
| 500 | 服务器内部错误 |
| 502 | AI 模型服务调用失败（降级提示） |

---

## 六、项目目录结构

### 6.1 后端目录结构（v2.0 新增 ai 模块）

```
cms-backend/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
│
├── src/main/java/com/cms/
│   ├── CmsApplication.java                 # 启动类
│   │
│   ├── config/                             # 配置类
│   │   ├── SecurityConfig.java             # 安全配置
│   │   ├── RedisConfig.java                # Redis配置
│   │   ├── MyBatisPlusConfig.java          # MyBatis配置
│   │   ├── MinioConfig.java                # MinIO配置
│   │   ├── Knife4jConfig.java              # API文档配置
│   │   ├── WebMvcConfig.java               # Web配置
│   │   ├── ThreadPoolConfig.java           # 线程池配置（AI 异步调用）
│   │   ├── AiModelConfig.java              # AI 模型配置（ChatClient Bean）
│   │   └── RateLimitConfig.java            # 限流配置（v2.0 新增）
│   │
│   ├── controller/                         # 控制器层
│   │   ├── auth/
│   │   │   └── AuthController.java         # 认证控制器（含 refresh/logout）
│   │   ├── admin/
│   │   │   ├── UserController.java         # 用户管理
│   │   │   ├── ArticleController.java      # 文章管理
│   │   │   ├── CategoryController.java     # 分类管理
│   │   │   ├── TagController.java          # 标签管理
│   │   │   ├── CommentController.java      # 评论管理（含人工复核）
│   │   │   ├── MediaController.java        # 媒体管理
│   │   │   └── StatsController.java        # 数据统计
│   │   ├── front/
│   │   │   ├── ArticleController.java      # 前台文章
│   │   │   ├── CategoryController.java     # 前台分类
│   │   │   └── CommentController.java      # 前台评论
│   │   └── ai/
│   │       ├── AiWriteController.java      # AI 写作（草稿/续写/润色）
│   │       ├── AiEnhanceController.java    # AI 摘要与SEO
│   │       └── AiChatController.java       # 站内问答（P1）
│   │
│   ├── service/                            # 业务层
│   │   ├── UserService.java
│   │   ├── impl/UserServiceImpl.java
│   │   ├── ArticleService.java
│   │   ├── impl/ArticleServiceImpl.java
│   │   ├── CategoryService.java
│   │   ├── impl/CategoryServiceImpl.java
│   │   ├── TagService.java
│   │   ├── impl/TagServiceImpl.java
│   │   ├── CommentService.java
│   │   ├── impl/CommentServiceImpl.java
│   │   ├── MediaService.java
│   │   ├── impl/MediaServiceImpl.java
│   │   ├── StatsService.java
│   │   ├── impl/StatsServiceImpl.java
│   │   ├── ai/
│   │   │   ├── AiWriteService.java         # AI 写作服务
│   │   │   ├── impl/AiWriteServiceImpl.java
│   │   │   ├── AiReviewService.java        # AI 评论审核服务
│   │   │   ├── impl/AiReviewServiceImpl.java
│   │   │   ├── AiGenerationRecordService.java # 生成记录
│   │   │   └── AiChatService.java          # 站内问答（P1）
│   │
│   ├── mapper/                             # 数据访问层
│   │   ├── UserMapper.java
│   │   ├── RoleMapper.java
│   │   ├── PermissionMapper.java
│   │   ├── ArticleMapper.java
│   │   ├── CategoryMapper.java
│   │   ├── TagMapper.java
│   │   ├── CommentMapper.java
│   │   ├── MediaMapper.java
│   │   ├── VisitLogMapper.java
│   │   ├── AiGenerationRecordMapper.java   # v2.0 新增
│   │   └── AiReviewRecordMapper.java       # v2.0 新增
│   │
│   ├── entity/                             # 实体类
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── Permission.java
│   │   ├── Article.java
│   │   ├── Category.java
│   │   ├── Tag.java
│   │   ├── Comment.java
│   │   ├── Media.java
│   │   ├── VisitLog.java
│   │   ├── AiGenerationRecord.java         # v2.0 新增
│   │   └── AiReviewRecord.java             # v2.0 新增
│   │
│   ├── dto/                                # 数据传输对象
│   │   ├── request/
│   │   │   ├── LoginReq.java
│   │   │   ├── RegisterReq.java
│   │   │   ├── RefreshTokenReq.java        # v2.0 新增
│   │   │   ├── ArticleCreateReq.java
│   │   │   ├── ArticleUpdateReq.java
│   │   │   ├── CommentCreateReq.java
│   │   │   ├── UserCreateReq.java
│   │   │   ├── AiDraftReq.java             # v2.0 新增
│   │   │   ├── AiContinueReq.java          # v2.0 新增
│   │   │   ├── AiPolishReq.java            # v2.0 新增
│   │   │   ├── AiEnhanceReq.java           # v2.0 新增
│   │   │   └── AiChatReq.java              # v2.0 新增
│   │   └── response/
│   │       ├── LoginResp.java              # 含 accessToken/refreshToken
│   │       ├── UserInfoResp.java
│   │       ├── ArticleResp.java
│   │       ├── ArticleDetailResp.java
│   │       ├── CategoryResp.java
│   │       ├── TagResp.java
│   │       ├── CommentResp.java
│   │       ├── StatsResp.java
│   │       ├── AiEnhanceResp.java          # v2.0 新增
│   │       └── AiChatResp.java             # v2.0 新增
│   │
│   ├── vo/                                 # 视图对象
│   │   ├── PageVO.java
│   │   └── Result.java
│   │
│   ├── common/                             # 公共模块
│   │   ├── ResultCode.java                 # 错误码
│   │   ├── BusinessException.java          # 业务异常
│   │   ├── GlobalExceptionHandler.java     # 全局异常处理
│   │   └── Constants.java                  # 常量
│   │
│   ├── security/                           # 安全模块
│   │   ├── JwtTokenProvider.java           # JWT工具（双令牌）
│   │   ├── JwtAuthenticationFilter.java    # JWT过滤器
│   │   ├── UserDetailsServiceImpl.java     # 用户认证服务
│   │   └── SecurityUtils.java              # 安全工具
│   │
│   ├── interceptor/                        # 拦截器
│   │   ├── VisitLogInterceptor.java        # 访问日志拦截器
│   │   └── RateLimitInterceptor.java       # 限流拦截器（v2.0 新增）
│   │
│   ├── ai/                                 # AI 能力模块
│   │   ├── prompt/                         # Prompt 模板管理
│   │   │   ├── PromptTemplateManager.java
│   │   │   └── PromptConstants.java
│   │   ├── streaming/                      # SSE 流式输出
│   │   │   └── SseEmitterManager.java
│   │   └── vector/                         # 向量检索（P1）
│   │       └── VectorStoreService.java
│   │
│   ├── util/                               # 工具类
│   │   ├── IpUtil.java
│   │   ├── JwtUtil.java
│   │   ├── FileUtil.java
│   │   └── HtmlSanitizer.java              # XSS 过滤（v2.0 新增）
│   │
│   └── job/                                # 定时任务
│       ├── StatsAggregateJob.java          # 统计聚合任务
│       └── ViewCountFlushJob.java          # 浏览量异步落库（v2.0 新增）
│
└── src/main/resources/
    ├── application.yml                     # 主配置
    ├── application-dev.yml                 # 开发环境
    ├── application-prod.yml                # 生产环境
    ├── prompts/                            # Prompt 模板文件（v2.0 新增）
    │   ├── draft.st
    │   ├── continue.st
    │   ├── polish.st
    │   ├── enhance.st
    │   ├── comment-review.st
    │   └── chat.st
    ├── mapper/                             # MyBatis映射文件
    │   ├── UserMapper.xml
    │   ├── ArticleMapper.xml
    │   └── StatsMapper.xml
    └── sql/
        └── init.sql                        # 建表脚本（含 v2.0 新增表）
```

### 6.2 前端目录结构（v2.0 新增 AI 页面）

```
cms-frontend/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
│
├── public/
│   └── favicon.ico
│
├── src/
│   ├── main.ts                           # 入口文件
│   ├── App.vue                           # 根组件
│   │
│   ├── api/                              # API接口
│   │   ├── index.ts                      # axios配置（含Token自动刷新）
│   │   ├── auth.ts                       # 认证接口（含 refresh）
│   │   ├── user.ts                       # 用户接口
│   │   ├── article.ts                    # 文章接口
│   │   ├── category.ts                   # 分类接口
│   │   ├── tag.ts                        # 标签接口
│   │   ├── comment.ts                    # 评论接口
│   │   ├── media.ts                      # 媒体接口
│   │   ├── stats.ts                      # 统计接口
│   │   └── ai.ts                         # AI 接口（v2.0 新增，SSE处理）
│   │
│   ├── assets/                           # 静态资源
│   │   ├── styles/
│   │   │   ├── index.scss                # 全局样式
│   │   │   └── variables.scss            # 变量
│   │   └── images/
│   │
│   ├── components/                       # 公共组件
│   │   ├── Layout/                       # 布局组件
│   │   │   ├── index.vue                 # 主布局
│   │   │   ├── Header.vue                # 头部
│   │   │   ├── Sidebar.vue               # 侧边栏
│   │   │   └── Footer.vue                # 底部
│   │   ├── Breadcrumb/                   # 面包屑
│   │   ├── Pagination/                   # 分页
│   │   ├── RichEditor/                   # 富文本编辑器
│   │   ├── ImageUpload/                  # 图片上传
│   │   ├── StatsChart/                   # 统计图表
│   │   └── AiGenerate/                   # AI 生成面板（v2.0 新增）
│   │
│   ├── views/                            # 页面组件
│   │   ├── login/                        # 登录页
│   │   │   └── index.vue
│   │   ├── dashboard/                    # 仪表盘
│   │   │   └── index.vue
│   │   ├── article/                      # 文章管理
│   │   │   ├── list.vue                  # 文章列表
│   │   │   └── edit.vue                  # 文章编辑（集成AI生成面板）
│   │   ├── category/                     # 分类管理
│   │   │   └── index.vue
│   │   ├── tag/                          # 标签管理
│   │   │   └── index.vue
│   │   ├── comment/                      # 评论管理（含AI机审状态展示）
│   │   │   └── index.vue
│   │   ├── media/                        # 媒体管理
│   │   │   └── index.vue
│   │   ├── user/                         # 用户管理
│   │   │   └── index.vue
│   │   ├── role/                         # 角色管理
│   │   │   └── index.vue
│   │   ├── stats/                        # 数据统计
│   │   │   └── index.vue
│   │   └── ai/                           # AI 中心（v2.0 新增）
│   │       ├── generations.vue           # 生成记录
│   │       └── chat.vue                  # 站内问答（P1）
│   │
│   ├── router/                           # 路由配置
│   │   ├── index.ts                      # 路由入口
│   │   └── permission.ts                 # 权限路由
│   │
│   ├── store/                            # 状态管理
│   │   ├── index.ts                      # store入口
│   │   ├── user.ts                       # 用户状态
│   │   ├── app.ts                        # 应用状态
│   │   └── permission.ts                 # 权限状态
│   │
│   ├── utils/                            # 工具函数
│   │   ├── request.ts                    # 请求封装（401自动刷新重试）
│   │   ├── auth.ts                       # 认证工具
│   │   ├── validate.ts                   # 验证工具
│   │   ├── storage.ts                    # 存储工具
│   │   └── sse.ts                        # SSE 解析（v2.0 新增）
│   │
│   ├── hooks/                            # 自定义hooks
│   │   ├── useAuth.ts
│   │   └── usePagination.ts
│   │
│   └── types/                            # 类型定义
│       ├── api.d.ts
│       ├── article.d.ts
│       ├── user.d.ts
│       ├── ai.d.ts                       # v2.0 新增
│       └── global.d.ts
│
└── .env.development                      # 开发环境变量
└── .env.production                       # 生产环境变量
```

---

## 七、核心功能实现

### 7.1 用户认证流程（v2.0 双令牌）

```
登录流程：
  1. 用户输入用户名和密码
  2. 前端发送POST请求到/api/auth/login
  3. 后端验证用户名和密码
  4. 验证通过，生成 AccessToken（有效期2小时）+ RefreshToken（有效期7天）
  5. RefreshToken 存入 Redis（key: refresh:token:{uuid}，带用户ID与有效期）
  6. 返回双 Token 和用户信息
  7. 前端存储 Token（RefreshToken 存入 localStorage）

Token 刷新流程：
  1. 前端请求返回 401（AccessToken 过期）
  2. 前端携带 RefreshToken 请求 /api/auth/refresh
  3. 后端校验 RefreshToken 签名 + Redis 中存在性 + 未注销
  4. 校验通过，以 Redis 原子操作（GETDEL）取用旧 RefreshToken，保证同一 Token 只能刷新成功一次（并发刷新兜底），签发新 AccessToken
  5. 前端用新 Token 重放失败请求

退出登录流程：
  1. 前端调用 /api/auth/logout
  2. 后端删除 Redis 中的 RefreshToken（使其立即失效）
  3. 前端清除本地 Token

密码重置流程（v2.1 新增）：
  1. 用户请求 /api/auth/forgot-password（输入注册邮箱）
  2. 后端生成一次性重置令牌，存入 Redis（key: reset:token:{token}，TTL 30分钟）
  3. 通过 Spring Mail 发送重置邮件，链接携带令牌
  4. 用户提交新密码 + 令牌 → /api/auth/reset-password
  5. 后端校验令牌有效性，更新密码（BCrypt 加密），删除令牌

权限验证流程：
  1. 前端发送请求，Header携带Authorization: Bearer {accessToken}
  2. JWT过滤器拦截请求
  3. 验证Token有效性（签名、过期时间）
  4. 解析用户信息和权限
  5. Spring Security验证权限（接口级鉴权）
  6. 权限通过，处理请求
```

### 7.2 文章发布流程（v2.0 支持 AI 辅助）

```
创建文章：
  1. 用户填写文章标题、内容、分类、标签（可先使用 AI 生成草稿，见7.4）
  2. 上传封面图到MinIO
  3. 选择发布状态（草稿/发布）
  4. 富文本内容经 XSS 过滤（HtmlSanitizer）后入库
  5. 前端发送POST请求到/api/articles
  6. 后端保存文章到数据库
  7. 保存文章标签关联
  8. 如果是发布状态，设置发布时间
  9. 清除文章缓存
  10. 返回文章信息
```

### 7.3 RBAC权限模型

```
用户(User) <---> 角色(Role) <---> 权限(Permission)

示例：
  - 超级管理员：拥有所有权限
  - 编辑：文章管理、分类管理、标签管理、评论管理、AI 生成
  - 作者：文章发布、文章编辑（自己的）、评论管理（自己的文章）、AI 生成
  - 普通用户：评论发布、文章浏览

数据级权限：
  - 文章修改/删除接口校验 author_id === 当前用户（或 ADMIN）
  - 越权访问返回 403
```

### 7.4 AI 写作助手（v2.0 新增）

```
功能：
  1. 按标题 + 关键词生成文章草稿（可选提供大纲）
  2. 基于已有内容续写
  3. 对已有内容润色改写
  4. 自动生成摘要 + SEO标题/描述/关键词

实现要点：
  1. Prompt 模板集中管理（resources/prompts/），使用 Spring AI PromptTemplate 渲染
  2. 通过 ChatClient 调用模型，SSE 流式返回（SseEmitter），前端逐字渲染
  3. 生成结果记录到 ai_generation_record（含模型、Token消耗，用于成本审计）
  4. 异步执行：AI 调用放入线程池，避免阻塞请求线程
  5. 降级策略：模型调用超时/失败 → 返回 502 + 提示前端"生成失败请重试"，不影响手动编辑
  6. 限流：AI 接口按用户维度限流（如每分钟5次），控制成本
  7. 生成内容仅作为辅助，前端提供"插入编辑器"按钮，用户确认后才保存
```

### 7.5 评论 AI 初审（v2.0 新增）

```
流程：
  1. 用户发表评论，评论先以 status=0（待审核）入库
  2. 异步调用 AI 模型对评论内容分类：normal正常 / ad广告 / repeat刷屏 / abuse辱骂 / sensitive敏感
  3. 分类结果与置信度写入 ai_review_record，更新 cms_comment.ai_status
  4. 自动流转：
     - normal（置信度高）→ status=1 已通过，前端可见
     - 疑似违规（ad/abuse/sensitive 或置信度低）→ 保持 status=0，转人工复核
     - 明显违规（置信度极高）→ status=2 已拒绝
  5. 管理员在评论管理页人工复核，可覆盖机审结果

实现要点：
  1. 要求模型返回结构化 JSON（{"result": "...", "score": 0.9, "reason": "..."}）
  2. 解析失败/超时 → 降级为 status=0 转人工，保证不漏审
  3. 使用 ai_review_record 保留审核明细，支持审计与召回分析
```

### 7.6 站内智能问答（P1 可选）

```
流程：
  1. 定时任务将已发布文章分块（按段落/标题切分），存入 ai_article_chunk（元数据）
  2. 分块内容向量化后存入 Milvus（VectorStore）
  3. 用户提问 → 问题向量化 → 向量检索 Top-K 相关分块 → 组装上下文调用模型生成答案
  4. 返回答案 + 来源文章列表（可点击跳转）

实现要点：
  1. 文章发布/更新时同步更新向量索引，删除时清理
  2. 使用 Spring AI VectorStore 抽象，切换向量库仅改配置
  3. 检索结果按相似度排序，答案末尾附来源，降低幻觉影响
```

### 7.7 缓存策略（v2.0 修正为 Cache-Aside + 延迟双删）

```
Redis缓存设计：
  - 文章详情：article:detail:{id}，TTL 30分钟
  - 文章列表：article:list:{page}:{size}:{categoryId}:{tagId}，TTL 10分钟
  - 分类列表：category:list，TTL 1小时
  - 标签列表：tag:list，TTL 1小时
  - 用户信息：user:info:{id}，TTL 30分钟
  - 浏览量计数：article:views:{id}，TTL 7天（原子自增，见7.7.1）
  - 点赞集合：article:liked:{id}，Set 存储点赞用户ID

读策略（Cache-Aside）：
  1. 先查缓存
  2. 未命中 → 查数据库 → 回写缓存（设置TTL）
  3. 命中 → 直接返回

写策略（延迟双删）：
  1. 更新数据库
  2. 删除缓存（第一次删除）
  3. 延迟约500ms后再删除一次（第二次删除）
     原因：并发下可能出现"读请求在第一次删除前读到旧值并回写缓存"，
           延迟删除可兜底清除这次回写的脏数据
  4. 删除失败的兜底：重试队列（Redis List + 定时任务补偿）

适用说明：缓存一致性存在极小时间窗口（毫秒级），对本项目读写场景可接受；
          对一致性要求更高的场景可引入消息队列串行化写操作。
```

#### 7.7.1 浏览量异步落库（v2.0 新增）

```
浏览量计数流程：
  1. 文章详情请求 → Redis INCR article:views:{articleId}（原子操作，避免行锁竞争）
  2. 展示层实时读 Redis 计数（或缓存值 + Redis 增量）
  3. 定时任务（ViewCountFlushJob，每5分钟）将增量批量 UPDATE 到 cms_article.view_count
  4. 应用启动时补偿：把 Redis 剩余增量落库，避免停机丢失

点赞防重流程：
  1. 点赞 → SADD article:liked:{id} {userId}，返回 1 表示首次点赞，0 表示已赞（幂等）
  2. 取消点赞 → SREM
  3. 点赞数展示 SCARD article:liked:{id}，定时同步到 cms_article.like_count
```

### 7.8 安全设计（v2.0 新增）

```
XSS 防护：
  - 富文本内容入库前使用 HtmlSanitizer（基于 JSoup 白名单）清洗：
    仅保留 p/strong/em/a/img/ul/ol/li/blockquote/code/pre/h1-h6 等安全标签
  - 移除 onclick/onerror 等事件属性与 javascript: 协议
  - 前端展示层默认转义（Vue 插值），富文本用 v-html 前必须经过后端清洗

文件上传安全：
  - 扩展名白名单：jpg/png/gif/webp/pdf/docx/xlsx 等
  - MIME 类型二次校验（不以扩展名信任）
  - 大小限制：图片 ≤ 5MB，文档 ≤ 20MB
  - 文件名服务端随机生成（UUID），防止路径穿越与重名覆盖
  - 上传目录与静态资源目录隔离

接口安全：
  - 登录接口限流（防暴力破解，按 IP 维度）
  - AI 接口按用户维度限流（控成本）
  - 数据级越权校验（见7.3）
  - SQL 注入：MyBatis-Plus 参数化查询，禁止字符串拼接
  - API Key 一律环境变量注入（DASHSCOPE_API_KEY），禁止硬编码与提交仓库
```

### 7.9 接口限流（v2.0 新增）

```
方案：Redis + Lua 脚本实现令牌桶算法
  - 每个受保护接口/维度维护一个令牌桶
  - 通过 Redis 分布式 Lua 脚本保证原子性（多实例部署同样生效）
  - 超过阈值的请求返回 429

维度与阈值（示例，可按需调整）：
  - 登录接口：IP 维度，10次/分钟
  - AI 生成接口：用户维度，5次/分钟
  - 评论接口：用户维度，20次/分钟
  - 普通接口：IP 维度，120次/分钟
```

### 7.10 数据删除策略（v2.1 新增）

```
删除文章：
  - 采用逻辑删除（is_deleted 标记）或物理删除 + 级联清理，实施前二选一定义
  - 级联清理内容：cms_article_tag 关联记录、该文章下的评论（及对应 ai_review_record）、ai_generation_record 关联记录、Redis 中的 article:detail:{id} 缓存与浏览量 key
  - 建议：文章关联数据多，采用逻辑删除（增加 is_deleted 字段，查询统一过滤）

删除用户：
  - 文章保留归属（作者昵称显示"已注销"）或一并处理，实施前定义
  - 评论保留并匿名化（显示"已注销用户"），保留审核记录

删除分类：
  - 若分类下存在文章：拒绝删除（返回 409）并提示先迁移文章
  - 建议采用拒绝删除，避免文章失去分类归属

删除媒体文件：
  - 先删除数据库记录，再删除 MinIO 对象（避免先删对象后记录删除失败导致引用断裂）
  - MinIO 删除失败：记录待清理任务，由定时任务补偿重试，避免孤儿文件
```

### 7.11 统计口径定义（v2.1 新增）

```
访问统计（/api/stats/visits）：
  - PV（页面浏览量）：sys_visit_log 原始请求计数，按日/按月聚合
  - UV（独立访客数）：按 user_id + ip_address 去重后计数
  - 时间粒度：支持按日、按月；返回 时间 + PV + UV 序列

文章统计（/api/stats/articles）：
  - 总数 / 已发布 / 草稿 / 已下架（按 status 分组）
  - 热门文章 TopN：按 view_count 排序（含 Redis 中未落库增量）

用户统计（/api/stats/users）：
  - 用户总数 / 按日、月新增用户数
  - 角色分布统计
```

---

## 八、开发计划（v2.0 加入 AI 阶段）

### 第一阶段：项目搭建（1周）

- [ ] 搭建后端Spring Boot项目骨架
- [ ] 搭建前端Vue3项目骨架
- [ ] 配置数据库、Redis、MinIO
- [ ] 实现基础配置类
- [ ] 创建数据库表（含 v2.0 新增表）

### 第二阶段：用户系统（1周）

- [ ] 实现用户注册、登录接口（双令牌）
- [ ] 实现 Token 刷新与退出登录
- [ ] 实现JWT认证和权限验证
- [ ] 实现用户管理CRUD
- [ ] 实现角色权限管理
- [ ] 前端登录页面
- [ ] 前端用户管理页面

### 第三阶段：文章系统（2周）

- [ ] 实现文章CRUD接口
- [ ] 实现分类管理接口
- [ ] 实现标签管理接口
- [ ] 实现浏览量异步计数、点赞防重
- [ ] 实现富文本 XSS 过滤（HtmlSanitizer）
- [ ] 前端文章列表页面
- [ ] 前端文章编辑页面（集成富文本编辑器：图片上传、代码高亮、Markdown导入）
- [ ] 前端分类管理页面
- [ ] 前端标签管理页面

### 第四阶段：评论系统（1周）

- [ ] 实现评论CRUD接口
- [ ] 实现评论审核状态机（待审核/通过/拒绝）
- [ ] 前端评论管理页面（含审核操作）
- [ ] 前端文章详情页评论展示

### 第五阶段：媒体管理（1周）

- [ ] 实现MinIO文件上传（白名单、大小校验、随机文件名）
- [ ] 实现媒体文件管理接口
- [ ] 前端媒体管理页面
- [ ] 前端图片上传组件

### 第六阶段：数据统计（1周）

- [ ] 实现访问日志记录
- [ ] 实现统计数据接口
- [ ] 前端仪表盘页面
- [ ] 前端数据统计页面（ECharts图表）

### 第七阶段：AI 智能能力（2周）

- [ ] 接入 Spring AI，配置模型（DashScope 主选 / Ollama 兜底）
- [ ] 实现 Prompt 模板管理与集中配置
- [ ] 实现 AI 写作助手（草稿/续写/润色，SSE 流式输出）
- [ ] 实现摘要与 SEO 自动生成
- [ ] 实现 AI 生成记录落库与成本审计
- [ ] 实现评论 AI 初审（结构化输出 + 状态流转 + 降级）
- [ ] 前端 AI 生成面板（文章编辑页集成）
- [ ] 前端 AI 生成记录页面
- [ ] AI 接口限流与失败降级

### 第八阶段：优化部署（1周）

- [ ] 缓存优化（延迟双删 + 删除失败补偿）
- [ ] 接口限流完善（Lua 令牌桶）
- [ ] Docker容器化（含模型 Key 环境变量注入）
- [ ] 性能压测（JMeter）与指标记录
- [ ] 核心接口编写 JUnit + MockMvc 测试（认证/文章/评论，v2.1 新增）
- [ ] 编写文档

### P1 扩展（可选，1-2周）

- [ ] 文章分块与向量化入库（Milvus）
- [ ] 站内智能问答接口与前端页面
- [ ] 来源文章引用与检索评估

**总计开发周期：10周（不含 P1 扩展）**

---

## 九、部署方案

### 9.1 Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY target/cms-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar","--spring.profiles.active=prod"]
```

### 9.2 docker-compose.yml（v2.0 新增 Key 注入与可选向量库）

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: cms
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  minio:
    image: minio/minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"

  backend:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - minio
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/cms
      SPRING_REDIS_HOST: redis
      MINIO_ENDPOINT: http://minio:9000
      DASHSCOPE_API_KEY: ${DASHSCOPE_API_KEY}   # 从宿主机环境变量注入，禁止写死
      # OLLAMA_BASE_URL: http://ollama:11434    # 使用 Ollama 时启用

  frontend:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./cms-frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - backend

  # P1 站内问答可选：向量库
  # milvus:
  #   image: milvusdb/milvus:v2.4-latest
  #   ports:
  #     - "19530:19530"
  #   volumes:
  #     - milvus_data:/var/lib/milvus

volumes:
  mysql_data:
  minio_data:
  # milvus_data:
```

### 9.3 部署说明

- 模型 API Key 通过宿主机环境变量 `DASHSCOPE_API_KEY` 注入容器，.env 文件加入 `.gitignore`
- 使用 Ollama 本地模型时，docker-compose 增加 ollama 服务并配置 `OLLAMA_BASE_URL`
- MinIO bucket 初始化：应用启动时通过 MinioClient 检测 bucket（bucketExists），不存在则自动创建（makeBucket），避免首次启动上传失败（v2.1 新增）
- 生产环境启用 HTTPS（Nginx 配置证书）

### 9.4 nginx.conf 示例（v2.1 新增）

```nginx
server {
    listen 80;
    server_name _;

    # 前端静态资源
    root /usr/share/nginx/html;
    index index.html;

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理到后端
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # AI 流式输出支持（SSE 不缓冲）
        proxy_buffering off;
        proxy_read_timeout 120s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|gif|svg|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public";
    }

    gzip on;
    gzip_types text/plain text/css application/json application/javascript image/svg+xml;
}
```

---

## 十、简历亮点提炼（v2.0 更新）

### 10.1 项目描述

> **CMS内容管理系统（AI智能内容助手）** | 独立开发
> 2026.06 至 2026.08
>
> 基于 Vue3 + Spring Boot 3.4 + Spring AI + MySQL + Redis + MinIO 独立开发的内容管理系统，支持文章管理、用户权限、数据统计、AI 智能助手等功能。
>
> - 设计RBAC权限模型，实现多角色权限控制，支持菜单、按钮、接口三级权限
> - 使用Spring Security + JWT实现无状态认证（AccessToken + RefreshToken 双令牌，支持自动刷新）
> - 集成TinyMCE富文本编辑器，支持图片上传、代码高亮、Markdown导入
> - 使用Redis缓存热点数据（Cache-Aside + 延迟双删），热点接口响应时间显著下降（JMeter 压测验证）
> - 集成Spring AI实现文章草稿生成、续写润色与摘要/SEO自动输出，SSE 流式返回
> - 基于Spring AI实现评论 AI 初审，机审后人工复核，形成完整审核闭环
> - 浏览量 Redis 原子自增 + 异步批量落库，点赞接口幂等防重
> - 集成MinIO对象存储，支持文件类型白名单与大小校验；富文本入库 XSS 过滤
> - 使用ECharts实现数据可视化，展示访问趋势、文章统计、用户增长
> - Docker Compose一键部署

### 10.2 技术亮点

| 技术点 | 实现方式 | 价值 |
|--------|----------|------|
| 权限控制 | RBAC + Spring Security（三级权限） | 多角色精细化管理 + 接口防越权 |
| 身份认证 | JWT 双令牌（Access + Refresh） | 无状态、可刷新、退出即时失效 |
| 数据缓存 | Redis + Cache-Aside + 延迟双删 | 高性能、缓存一致性有兜底 |
| AI 能力 | Spring AI + DashScope/Ollama | 文章生成、内容审核智能化 |
| AI 审核 | 结构化输出 + 状态机 + 降级人工 | 审核效率提升、不漏审 |
| 并发处理 | Redis 原子计数 + 异步落库 | 高并发下计数稳定 |
| 文件存储 | MinIO对象存储 | 可扩展、高可用 |
| 数据可视化 | ECharts图表 | 直观展示统计结果 |
| 安全防护 | XSS过滤 + 上传校验 + 限流 | 满足生产级安全基线 |
| API文档 | Knife4j | 接口文档自动化 |

---

## 附录

### A. 配置文件示例

#### application.yml（v2.0 更新）

```yaml
server:
  port: 8080

spring:
  profiles:
    active: dev

  datasource:
    url: jdbc:mysql://localhost:3306/cms?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379
      database: 0

  mail:
    host: smtp.example.com
    port: 465
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    default-encoding: UTF-8

  # Spring AI 配置（主选 DashScope，切换 Ollama 见注释）
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
          temperature: 0.7
    # ollama:
    #   base-url: http://localhost:11434
    #   chat:
    #     options:
    #       model: qwen2.5:7b

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

# JWT配置（双令牌）
jwt:
  secret: your-secret-key-here
  access-token-expiration: 7200000      # AccessToken 有效期 2小时
  refresh-token-expiration: 604800000   # RefreshToken 有效期 7天

# MinIO配置
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: cms

# AI 限流配置
rate-limit:
  login-per-minute: 10
  ai-per-minute: 5
  comment-per-minute: 20

# 定时任务
job:
  view-count-flush-interval-ms: 300000   # 浏览量落库间隔 5分钟

# Knife4j配置
knife4j:
  enable: true
```

### B. AI 依赖（pom.xml 片段）

```xml
<!-- Spring AI 依赖（版本由 Spring Boot BOM 管理） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-dashscope</artifactId>
</dependency>

<!-- 如使用 Ollama 本地模型 -->
<!--
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
-->
```

### C. Prompt 模板示例（resources/prompts/draft.st）

```text
你是一名资深技术博客作者，请根据以下要求生成文章草稿。

标题：{title}
关键词：{keywords}
{outline_block}

要求：
1. 文章结构清晰，有引言、正文分节、总结
2. 语言专业、易懂，避免空话套话
3. 只输出文章正文内容，使用 Markdown 格式
```

### D. 性能指标与验证方式（v2.0 新增，审计口径）

| 指标 | 目标值 | 验证方式 |
|------|--------|----------|
| 热点接口响应时间（缓存命中） | P95 < 50ms | JMeter 压测：文章详情/分类列表接口，并发50，持续5分钟 |
| 缓存命中率 | > 90% | Redis INFO stats 统计 |
| AI 草稿生成首字延迟 | < 3s | 接入层日志统计 |
| 评论机审平均耗时 | < 5s（异步） | ai_review_record.create_time 统计 |
| 接口限流生效 | 超阈值返回 429 | 限流测试用例 |

> 说明：以上目标值为规划基线，开发完成后按此口径实测记录，实际结果可能因环境差异浮动；未实测前不作为对外承诺数据。

---

> **文档结束（v2.1，2026-09-29 补充附录 E 前端设计系统）**
> 如需开始编码实现，请告诉我从哪个阶段开始。

---

## 附录 E：前端设计系统（编辑部 Editorial，2026-09-29 补充）

> 设计语言完整规范见项目根目录 `CMS前端设计系统_v1.0.md`（273 行，11 章）；本附录为摘要 + 落地状态，作为需求文档内的设计约束，前端页面开发一律按此执行。

### E.1 设计语言（四槽位）

| 槽位 | 取值 |
|---|---|
| World | 编辑部 / 稿件工作台 |
| Materials | 纸张白、墨色、排版网格、编辑批注红 |
| Palette | 墨绿主色 + 暖纸白底 + 低饱和语义色（避开奶油+陶土、紫蓝渐变、近黑+荧光） |
| Signature | 稿件状态色带（草稿灰/审核琥珀/发布绿/驳回红）+ 稿件编号 |

### E.2 色彩令牌（关键值）

| 令牌 | 值 | 用途 |
|---|---|---|
| `--bg-page` | `#F6F4EF` | 页面底色（暖纸白） |
| `--text-primary` | `#23262B` | 墨黑文本（禁纯黑 #0B0B0B） |
| `--border-default` | `#E4E0D8` | 暖灰边框 |
| `--brand-primary` | `#1F6B5A` | 墨绿主色（覆盖 Element Plus 默认蓝） |
| `--brand-light` | `#E7F0ED` | 主色浅底 |
| 语义色 | `#3E7C4F` / `#C98A2D` / `#C0453C` | 成功 / 警告 / 错误（低饱和） |
| 状态色带 | `#9AA1AB` / `#C98A2D` / `#3E7C4F` / `#C0453C` | 草稿 / 审核中 / 已发布 / 已驳回 |

### E.3 字体与几何

- UI 字体：`'PingFang SC','HarmonyOS Sans SC','Microsoft YaHei UI','Segoe UI',sans-serif`
- 展示字体（仅登录页/仪表盘大标题，全站 ≤ 3 处）：`'Noto Serif SC','Source Han Serif SC','Songti SC',serif`
- 数据字体：等宽（JetBrains Mono/Consolas），数字统一 `tabular-nums`
- 圆角分级：列表 0 / 按钮 4 / 卡片 8 / 大卡 12（**禁止统一圆角**）
- 阴影：默认无阴影，1px 暖灰边框区分；仅弹窗保留 `shadow-md`
- 间距：8px 基准（4/8/12/16/24/32/48/64）

### E.4 反 AI 痕迹清单（每页交付前自查）

1. 禁紫蓝渐变 / 发光光晕 / 玻璃拟态
2. 禁奶油底 + 衬线大字 + 陶土强调的组合（本设计为暖纸白 + 墨绿）
3. 禁近黑背景 + 荧光强调；侧边栏/顶栏禁深色底
4. 圆角必须分级，禁所有元素同一圆角
5. 禁所有卡片同款灰阴影（默认无阴影）
6. 禁标题上方全大写眉标、禁"A·B·C"中点分隔、禁按钮文字加"→"
7. 编号 01/02/03 仅真实序列使用
8. Element Plus 默认蓝 `#409EFF` 全局覆盖为墨绿
9. 动效只保留 2 处：页面切换 fade 200ms + 仪表盘数字入场一次编排

### E.5 落地状态（cms-frontend）

- 骨架已实现：登录页、管理后台 Layout（侧边栏/顶栏/页脚）、仪表盘、文章管理（稿件台签名元素）、评论管理（AI 机审状态展示）、文章编辑占位、其余页面空态占位
- Element Plus 主题覆盖：`src/assets/styles/index.scss`（CSS 变量 + 组件级微调）
- 设计令牌唯一来源：`src/assets/styles/variables.scss`
- 认证联调：mock 后端（`cms-frontend/mock-server/server.mjs`，接口契约对齐 §5.1），前端 401 自动刷新已实现（`src/utils/request.ts`）
- 演示/联调开关：`.env.development` 中 `VITE_DEMO_MODE`（true=演示假登录，false=真实 /api/auth/login 链路）
