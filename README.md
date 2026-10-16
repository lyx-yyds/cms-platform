# 智能内容创作与管理系统（CMS）

一个前后端分离的内容创作与管理平台，面向"编辑部"场景：作者撰稿、编辑审核、发布、评论管理、AI 辅助写作与评论初审。

## 技术栈

**后端**
- Spring Boot 3.4 / Java 17
- Spring Security + JWT（双令牌：Access + Refresh）
- MyBatis-Plus + MySQL
- Spring AI 1.x（DashScope `qwen-plus` 主选，本地 Ollama `qwen2.5:3b` 兜底）
- Redis（缓存 / Token 防重放 / 浏览量计数）
- MinIO（对象存储）
- Knife4j（接口文档）

**前端**
- Vue 3 + TypeScript + Vite
- Element Plus + Pinia + Vue Router
- ECharts（数据看板）
- wangEditor（富文本编辑器）

## 核心功能

- 文章全生命周期：草稿 / 提交 / 审核 / 发布 / 下架，分类与标签管理
- 评论闭环：用户发表 → AI 初审（关键词规则 + 模型分类）→ 通过公开展示 / 疑似转人工审核
- 用户与权限：基于**权限点**的 RBAC，角色（ADMIN / EDITOR / AUTHOR / USER）与权限点解耦，前后端同时做按钮级与接口级鉴权
- AI 能力：写作助手（SSE 流式输出）、文章摘要、评论机审
- 数据看板：阅读量、评论数、分类分布等可视化统计
- 媒体库：图片上传、裁剪、MinIO 托管

## 架构亮点

- **JWT 双令牌 + 刷新令牌防重放**：RefreshToken 携带 `jti` 写入 Redis，刷新时通过 `GETDEL` 原子取用，保证同一 RefreshToken 只能成功使用一次；前端 axios 响应拦截器捕获 401 自动换新并重试原请求。
- **权限点驱动鉴权**：不依赖粗粒度角色字符串，而是将"文章创建 / 评论审核 / 用户管理 / AI 调用"等拆为独立权限点，`@PreAuthorize("hasAuthority('article:create')")` 控制接口，前端根据权限点动态渲染按钮与菜单。
- **AI 双通道 + 兜底**：主链路调用云端 DashScope，主链路异常或未配置 Key 时自动降级到本地 Ollama，保证功能可独立运行演示。
- **浏览量高并发优化**：阅读计数先写 Redis 并定时批量刷盘到 MySQL，避免每次阅读都打数据库。

## 项目结构

```
├── cms-backend/          # Spring Boot 后端
│   ├── src/main/java/com/cms/
│   │   ├── controller/   # REST 接口
│   │   ├── service/      # 业务逻辑
│   │   ├── security/     # JWT 过滤器、权限装配
│   │   └── ai/           # Spring AI 调用
│   └── src/main/resources/
│       ├── application.yml           # 本地配置（已 gitignore，勿提交）
│       └── application-example.yml   # 配置模板
├── cms-frontend/         # Vue 3 前端
└── 启动指南.md            # 详细启动说明
```

## 本地启动

### 前置依赖
- JDK 17+
- MySQL 8+，创建数据库 `cms`
- Redis
- MinIO，创建 bucket `cms`
- （可选）Ollama 拉取模型 `qwen2.5:3b`

### 后端
```bash
cd cms-backend
cp src/main/resources/application-example.yml src/main/resources/application.yml
# 编辑 application.yml，填入 MySQL 密码 / DashScope Key（可选）
mvn spring-boot:run
```

后端默认端口 `8080`，接口文档：http://localhost:8080/doc.html

### 前端
```bash
cd cms-frontend
npm install
npm run dev
```

前端默认端口 `5173`。

## 接口文档

后端启动后访问 Knife4j：http://localhost:8080/doc.html
