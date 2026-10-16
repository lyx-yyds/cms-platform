# CMS内容管理系统 - 项目设计文档

> 项目名称：cms-system
> 文档版本：v1.0
> 创建日期：2026-06-22

---

## 一、项目概述

### 1.1 项目背景

内容管理系统（Content Management System）是企业和个人管理网站内容的核心工具。本项目旨在开发一个功能完整的CMS系统，涵盖文章管理、用户权限、数据统计等核心功能。

### 1.2 项目目标

- 实现文章发布、编辑、删除、分类、标签等核心功能
- 支持RBAC权限控制，实现多角色管理
- 提供数据统计和可视化功能
- 支持媒体文件上传和管理
- 具备良好的扩展性和可维护性

### 1.3 核心功能清单

| 模块 | 功能 |
|------|------|
| 文章管理 | 文章发布、编辑、删除、分类、标签、草稿箱 |
| 用户管理 | 用户列表、角色管理、权限控制 |
| 评论系统 | 评论发布、评论审核、评论管理 |
| 数据统计 | 访问量统计、用户统计、文章统计 |
| 媒体管理 | 图片上传、文件管理、媒体库 |
| 系统设置 | 网站配置、邮件配置、SEO设置 |

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
| Spring Boot | 3.x | 后端框架 |
| MyBatis-Plus | 3.x | ORM框架 |
| MySQL | 8.0 | 数据库 |
| Redis | 7.x | 缓存 |
| MinIO | 最新 | 对象存储 |
| Spring Security | 6.x | 安全框架 |
| JWT | - | 身份认证 |
| Knife4j | 4.x | API文档 |

### 2.3 开发工具

| 工具 | 说明 |
|------|------|
| IDEA | Java开发IDE |
| VS Code | 前端开发IDE |
| Navicat | 数据库管理 |
| Git | 版本控制 |
| Maven | 项目构建 |
| Docker | 容器化部署 |

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
```

### 3.2 请求处理链路

```
前端请求：
  浏览器 → Nginx → Vue3应用 → API请求 → Spring Boot → MySQL/Redis

文件上传：
  浏览器 → Nginx → Spring Boot → MinIO → 返回文件URL

用户认证：
  登录请求 → Spring Boot → 验证密码 → 生成JWT → 返回Token
  后续请求 → 携带Token → Spring Boot → 验证JWT → 处理请求
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
│ role_id (FK) │       │ status           │       │ status           │
│ create_time  │       │ view_count       │       │ create_time      │
└──────────────┘       │ create_time      │       └──────────────────┘
                       └──────────────────┘
                              │
                              │ 聚合
                              ▼
                       ┌──────────────────┐
                       │    category      │
                       ├──────────────────┤
                       │ id (PK)          │
                       │ name             │
                       │ slug             │
                       │ description      │
                       └──────────────────┘
```

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
  `view_count`     INT             DEFAULT 0 COMMENT '浏览量',
  `like_count`     INT             DEFAULT 0 COMMENT '点赞数',
  `comment_count`  INT             DEFAULT 0 COMMENT '评论数',
  `seo_title`      VARCHAR(200)    DEFAULT NULL COMMENT 'SEO标题',
  `seo_description` VARCHAR(500)   DEFAULT NULL COMMENT 'SEO描述',
  `seo_keywords`   VARCHAR(200)    DEFAULT NULL COMMENT 'SEO关键词',
  `publish_time`   DATETIME        DEFAULT NULL COMMENT '发布时间',
  `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`)
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
  `name`         VARCHAR(200)    NOT NULL COMMENT '文件名',
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

### 4.3 索引设计说明

| 表 | 索引 | 类型 | 用途 |
|---|---|---|---|
| sys_user | uk_username | 唯一索引 | 保证用户名唯一 |
| sys_user | uk_email | 唯一索引 | 保证邮箱唯一 |
| cms_article | idx_category_id | 普通索引 | 按分类查询文章 |
| cms_article | idx_author_id | 普通索引 | 按作者查询文章 |
| cms_article | idx_status | 普通索引 | 按状态筛选文章 |
| cms_article | idx_publish_time | 普通索引 | 按发布时间排序 |
| cms_comment | idx_article_id | 普通索引 | 按文章查询评论 |
| cms_comment | idx_user_id | 普通索引 | 按用户查询评论 |

---

## 五、核心接口设计

### 5.1 接口总览

#### 用户接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 用户登录 | POST | `/api/auth/login` | 否 | 返回JWT |
| 用户注册 | POST | `/api/auth/register` | 否 | 用户注册 |
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
| 修改文章 | PUT | `/api/articles/{id}` | 是 | 修改文章 |
| 删除文章 | DELETE | `/api/articles/{id}` | 是 | 删除文章 |
| 文章点赞 | POST | `/api/articles/{id}/like` | 是 | 点赞文章 |
| 我的文章 | GET | `/api/articles/mine` | 是 | 查询我的文章 |

#### 分类接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 分类列表 | GET | `/api/categories` | 否 | 查询所有分类 |
| 创建分类 | POST | `/api/categories` | 是(ADMIN) | 创建分类 |
| 修改分类 | PUT | `/api/categories/{id}` | 是(ADMIN) | 修改分类 |
| 删除分类 | DELETE | `/api/categories/{id}` | 是(ADMIN) | 删除分类 |

#### 标签接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 标签列表 | GET | `/api/tags` | 否 | 查询所有标签 |
| 创建标签 | POST | `/api/tags` | 是 | 创建标签 |
| 删除标签 | DELETE | `/api/tags/{id}` | 是(ADMIN) | 删除标签 |

#### 评论接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 文章评论列表 | GET | `/api/articles/{id}/comments` | 否 | 查询文章评论 |
| 发表评论 | POST | `/api/articles/{id}/comments` | 是 | 发表评论 |
| 删除评论 | DELETE | `/api/comments/{id}` | 是 | 删除评论 |
| 审核评论 | PUT | `/api/admin/comments/{id}/audit` | 是(ADMIN) | 审核评论 |
| 评论列表 | GET | `/api/admin/comments` | 是(ADMIN) | 查询所有评论 |

#### 媒体接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 上传文件 | POST | `/api/media/upload` | 是 | 上传文件 |
| 文件列表 | GET | `/api/media` | 是 | 查询文件列表 |
| 删除文件 | DELETE | `/api/media/{id}` | 是 | 删除文件 |

#### 统计接口

| 接口 | Method | Path | 鉴权 | 说明 |
|------|--------|------|------|------|
| 访问统计 | GET | `/api/stats/visits` | 是(ADMIN) | 访问量统计 |
| 文章统计 | GET | `/api/stats/articles` | 是(ADMIN) | 文章统计 |
| 用户统计 | GET | `/api/stats/users` | 是(ADMIN) | 用户统计 |
| 仪表盘数据 | GET | `/api/stats/dashboard` | 是(ADMIN) | 仪表盘概览 |

### 5.2 接口示例

#### 用户登录

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
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
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

#### 文章列表

```json
// GET /api/articles?page=1&size=10&categoryId=1

// Response
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "title": "Vue3入门指南",
        "summary": "本文介绍Vue3的基本概念...",
        "coverImage": "http://xxx.com/cover.jpg",
        "category": {
          "id": 1,
          "name": "前端开发"
        },
        "tags": [
          {"id": 1, "name": "Vue"},
          {"id": 2, "name": "JavaScript"}
        ],
        "author": {
          "id": 1,
          "nickname": "张三",
          "avatar": "http://xxx.com/avatar.jpg"
        },
        "viewCount": 1234,
        "likeCount": 56,
        "commentCount": 78,
        "publishTime": "2026-06-22 10:00:00"
      }
    ]
  }
}
```

#### 文章详情

```json
// GET /api/articles/1

// Response
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "Vue3入门指南",
    "content": "<p>本文介绍Vue3的基本概念...</p>",
    "summary": "本文介绍Vue3的基本概念...",
    "coverImage": "http://xxx.com/cover.jpg",
    "category": {
      "id": 1,
      "name": "前端开发"
    },
    "tags": [
      {"id": 1, "name": "Vue"},
      {"id": 2, "name": "JavaScript"}
    ],
    "author": {
      "id": 1,
      "nickname": "张三",
      "avatar": "http://xxx.com/avatar.jpg"
    },
    "viewCount": 1235,
    "likeCount": 56,
    "commentCount": 78,
    "isLiked": false,
    "publishTime": "2026-06-22 10:00:00"
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
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

---

## 六、项目目录结构

### 6.1 后端目录结构

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
│   │   └── ThreadPoolConfig.java           # 线程池配置
│   │
│   ├── controller/                         # 控制器层
│   │   ├── auth/
│   │   │   └── AuthController.java         # 认证控制器
│   │   ├── admin/
│   │   │   ├── UserController.java         # 用户管理
│   │   │   ├── ArticleController.java      # 文章管理
│   │   │   ├── CategoryController.java     # 分类管理
│   │   │   ├── TagController.java          # 标签管理
│   │   │   ├── CommentController.java      # 评论管理
│   │   │   ├── MediaController.java        # 媒体管理
│   │   │   └── StatsController.java        # 数据统计
│   │   └── front/
│   │       ├── ArticleController.java      # 前台文章
│   │       ├── CategoryController.java     # 前台分类
│   │       └── CommentController.java      # 前台评论
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
│   │   └── impl/StatsServiceImpl.java
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
│   │   └── VisitLogMapper.java
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
│   │   └── VisitLog.java
│   │
│   ├── dto/                                # 数据传输对象
│   │   ├── request/
│   │   │   ├── LoginReq.java
│   │   │   ├── RegisterReq.java
│   │   │   ├── ArticleCreateReq.java
│   │   │   ├── ArticleUpdateReq.java
│   │   │   ├── CommentCreateReq.java
│   │   │   └── UserCreateReq.java
│   │   └── response/
│   │       ├── LoginResp.java
│   │       ├── UserInfoResp.java
│   │       ├── ArticleResp.java
│   │       ├── ArticleDetailResp.java
│   │       ├── CategoryResp.java
│   │       ├── TagResp.java
│   │       ├── CommentResp.java
│   │       └── StatsResp.java
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
│   │   ├── JwtTokenProvider.java           # JWT工具
│   │   ├── JwtAuthenticationFilter.java    # JWT过滤器
│   │   ├── UserDetailsServiceImpl.java     # 用户认证服务
│   │   └── SecurityUtils.java              # 安全工具
│   │
│   ├── interceptor/                        # 拦截器
│   │   └── VisitLogInterceptor.java        # 访问日志拦截器
│   │
│   ├── util/                               # 工具类
│   │   ├── IpUtil.java
│   │   ├── JwtUtil.java
│   │   └── FileUtil.java
│   │
│   └── job/                                # 定时任务
│       └── StatsAggregateJob.java          # 统计聚合任务
│
└── src/main/resources/
    ├── application.yml                     # 主配置
    ├── application-dev.yml                 # 开发环境
    ├── application-prod.yml                # 生产环境
    ├── mapper/                             # MyBatis映射文件
    │   ├── UserMapper.xml
    │   ├── ArticleMapper.xml
    │   └── StatsMapper.xml
    └── sql/
        └── init.sql                        # 建表脚本
```

### 6.2 前端目录结构

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
│   │   ├── index.ts                      # axios配置
│   │   ├── auth.ts                       # 认证接口
│   │   ├── user.ts                       # 用户接口
│   │   ├── article.ts                    # 文章接口
│   │   ├── category.ts                   # 分类接口
│   │   ├── tag.ts                        # 标签接口
│   │   ├── comment.ts                    # 评论接口
│   │   ├── media.ts                      # 媒体接口
│   │   └── stats.ts                      # 统计接口
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
│   │   └── StatsChart/                   # 统计图表
│   │
│   ├── views/                            # 页面组件
│   │   ├── login/                        # 登录页
│   │   │   └── index.vue
│   │   ├── dashboard/                    # 仪表盘
│   │   │   └── index.vue
│   │   ├── article/                      # 文章管理
│   │   │   ├── list.vue                  # 文章列表
│   │   │   └── edit.vue                  # 文章编辑
│   │   ├── category/                     # 分类管理
│   │   │   └── index.vue
│   │   ├── tag/                          # 标签管理
│   │   │   └── index.vue
│   │   ├── comment/                      # 评论管理
│   │   │   └── index.vue
│   │   ├── media/                        # 媒体管理
│   │   │   └── index.vue
│   │   ├── user/                         # 用户管理
│   │   │   └── index.vue
│   │   ├── role/                         # 角色管理
│   │   │   └── index.vue
│   │   └── stats/                        # 数据统计
│   │       └── index.vue
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
│   │   ├── request.ts                    # 请求封装
│   │   ├── auth.ts                       # 认证工具
│   │   ├── validate.ts                   # 验证工具
│   │   └── storage.ts                    # 存储工具
│   │
│   ├── hooks/                            # 自定义hooks
│   │   ├── useAuth.ts
│   │   └── usePagination.ts
│   │
│   └── types/                            # 类型定义
│       ├── api.d.ts
│       ├── article.d.ts
│       ├── user.d.ts
│       └── global.d.ts
│
└── .env.development                      # 开发环境变量
└── .env.production                       # 生产环境变量
```

---

## 七、核心功能实现

### 7.1 用户认证流程

```
登录流程：
  1. 用户输入用户名和密码
  2. 前端发送POST请求到/api/auth/login
  3. 后端验证用户名和密码
  4. 验证通过，生成JWT Token
  5. 返回Token和用户信息
  6. 前端存储Token到localStorage
  7. 后续请求携带Token

权限验证流程：
  1. 前端发送请求，Header携带Authorization: Bearer {token}
  2. JWT过滤器拦截请求
  3. 验证Token有效性
  4. 解析用户信息和权限
  5. Spring Security验证权限
  6. 权限通过，处理请求
```

### 7.2 文章发布流程

```
创建文章：
  1. 用户填写文章标题、内容、分类、标签
  2. 上传封面图到MinIO
  3. 选择发布状态（草稿/发布）
  4. 前端发送POST请求到/api/articles
  5. 后端保存文章到数据库
  6. 保存文章标签关联
  7. 如果是发布状态，设置发布时间
  8. 清除文章缓存
  9. 返回文章信息
```

### 7.3 RBAC权限模型

```
用户(User) <---> 角色(Role) <---> 权限(Permission)

示例：
  - 超级管理员：拥有所有权限
  - 编辑：文章管理、分类管理、标签管理、评论管理
  - 作者：文章发布、文章编辑（自己的）、评论管理（自己的文章）
  - 普通用户：评论发布、文章浏览
```

### 7.4 缓存策略

```
Redis缓存设计：
  - 文章详情：article:detail:{id}，TTL 30分钟
  - 文章列表：article:list:{page}:{size}:{categoryId}:{tagId}，TTL 10分钟
  - 分类列表：category:list，TTL 1小时
  - 标签列表：tag:list，TTL 1小时
  - 用户信息：user:info:{id}，TTL 30分钟
  - 访问计数：article:views:{id}:{date}，TTL 7天

缓存更新策略：
  - 写操作：先更新数据库，再删除缓存
  - 读操作：先查缓存，未命中查数据库并回写缓存
```

---

## 八、开发计划

### 第一阶段：项目搭建（1周）

- [ ] 搭建后端Spring Boot项目骨架
- [ ] 搭建前端Vue3项目骨架
- [ ] 配置数据库、Redis、MinIO
- [ ] 实现基础配置类
- [ ] 创建数据库表

### 第二阶段：用户系统（1周）

- [ ] 实现用户注册、登录接口
- [ ] 实现JWT认证和权限验证
- [ ] 实现用户管理CRUD
- [ ] 实现角色权限管理
- [ ] 前端登录页面
- [ ] 前端用户管理页面

### 第三阶段：文章系统（2周）

- [ ] 实现文章CRUD接口
- [ ] 实现分类管理接口
- [ ] 实现标签管理接口
- [ ] 前端文章列表页面
- [ ] 前端文章编辑页面（集成富文本编辑器）
- [ ] 前端分类管理页面
- [ ] 前端标签管理页面

### 第四阶段：评论系统（1周）

- [ ] 实现评论CRUD接口
- [ ] 实现评论审核功能
- [ ] 前端评论管理页面
- [ ] 前端文章详情页评论展示

### 第五阶段：媒体管理（1周）

- [ ] 实现MinIO文件上传
- [ ] 实现媒体文件管理接口
- [ ] 前端媒体管理页面
- [ ] 前端图片上传组件

### 第六阶段：数据统计（1周）

- [ ] 实现访问日志记录
- [ ] 实现统计数据接口
- [ ] 前端仪表盘页面
- [ ] 前端数据统计页面（ECharts图表）

### 第七阶段：优化部署（1周）

- [ ] 缓存优化
- [ ] 接口限流
- [ ] Docker容器化
- [ ] 编写文档

**总计开发周期：8周**

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

### 9.2 docker-compose.yml

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

  frontend:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./cms-frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - backend

volumes:
  mysql_data:
  minio_data:
```

---

## 十、简历亮点提炼

### 10.1 项目描述

> **CMS内容管理系统** | 独立开发
> 2026-XX 至 2026-XX
>
> 基于 Vue3 + Spring Boot + MySQL + Redis + MinIO 独立开发的内容管理系统，支持文章管理、用户权限、数据统计等功能。
>
> - 设计RBAC权限模型，实现多角色权限控制，支持菜单、按钮、接口三级权限
> - 使用Spring Security + JWT实现无状态认证，支持Token自动刷新
> - 集成TinyMCE富文本编辑器，支持图片上传、代码高亮、Markdown导入
> - 使用Redis缓存热点数据，接口响应时间从200ms降至20ms
> - 集成MinIO对象存储，支持图片、文件上传和管理
> - 使用ECharts实现数据可视化，展示访问趋势、文章统计、用户增长
> - Docker Compose一键部署，支持快速上线

### 10.2 技术亮点

| 技术点 | 实现方式 | 价值 |
|--------|----------|------|
| 权限控制 | RBAC + Spring Security | 多角色精细化管理 |
| 身份认证 | JWT + Token刷新 | 无状态、高性能 |
| 数据缓存 | Redis + 延迟双删 | 高性能、数据一致性 |
| 文件存储 | MinIO对象存储 | 可扩展、高可用 |
| 数据可视化 | ECharts图表 | 直观展示统计结果 |
| API文档 | Knife4j | 接口文档自动化 |

---

## 附录

### A. 配置文件示例

#### application.yml

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

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

# JWT配置
jwt:
  secret: your-secret-key-here
  expiration: 86400000  # 24小时

# MinIO配置
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: cms

# Knife4j配置
knife4j:
  enable: true
```

---

> **文档结束**
> 如需开始编码实现，请告诉我从哪个阶段开始。
