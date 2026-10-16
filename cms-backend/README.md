# CMS 后端骨架（cms-backend）

CMS 内容管理系统后端，按《CMS内容管理系统_项目设计文档_v2.1.md》第一阶段（后端骨架）实现。

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| Java | 17（本机 JDK 21 编译 release 17） | — |
| Spring Boot | 3.4.5 | — |
| Spring Security 6 | BOM 管理 | BCrypt + 双令牌 JWT |
| MyBatis-Plus | 3.5.9 + mybatis-plus-jsqlparser | 分页插件独立模块 |
| MySQL | 8.0 | 驱动 mysql-connector-j |
| Redis | 7 | RefreshToken / 重置令牌 / 缓存（后续） |
| MinIO | 8.5.12 | 媒体文件（第三阶段） |
| JJWT | 0.12.6 | api/impl/jackson |
| Knife4j | 4.5.0 | /doc.html API 文档 |
| Spring AI | —（第七阶段启用，pom 已注释预留） | DashScope 主选 + Ollama 兜底 |

## 目录结构（对齐文档 6.1）

```
cms-backend/
├── pom.xml                        # Maven 依赖（Spring AI 注释预留）
├── Dockerfile                     # 多阶段构建（第八阶段）
├── docker-compose.yml             # mysql/redis/minio/backend/nginx（第八阶段）
├── src/main/java/com/cms/
│   ├── CmsApplication.java
│   ├── common/                    # Result / ResultCode / 异常 / 常量
│   ├── config/                    # Security / Redis / MyBatisPlus / Minio / Knife4j / WebMvc
│   ├── security/                  # JwtTokenProvider / JwtAuthenticationFilter / UserDetailsServiceImpl / SecurityUtils
│   ├── entity/                    # User / Role / Permission
│   ├── mapper/                    # UserMapper / RoleMapper / PermissionMapper（RBAC 联查）
│   ├── dto/                       # request（Login/Register/Refresh/Forgot/Reset）+ response（LoginResp/UserInfoResp）
│   ├── service/                   # UserService + impl
│   └── controller/                # AuthController（auth/* + user/info）
└── src/main/resources/
    ├── application.yml            # 通用配置（dev/prod 两套 profile）
    ├── application-dev.yml        # 本地开发（localhost）
    ├── application-prod.yml       # 生产（环境变量注入，Docker）
    └── sql/init.sql               # 15 张表全量 + 种子数据（admin/admin123）
```

## 已实现

### 第一阶段：项目骨架 + 认证系统（✅ 已完成并验证）

- **双令牌认证**（文档 7.1）：`/api/auth/login` 签发 AccessToken(2h) + RefreshToken(7d)；
  RefreshToken 存入 Redis `refresh:token:{userId}:{jti}`；`/api/auth/refresh` 用 **GETDEL 原子取用防重放**，成功后轮换新对；
  `/api/auth/logout` 清理该用户全部 RefreshToken。
- **注册**：默认 USER 角色（RBAC user→role 多对多，文档 7.3）。
- **密码重置闭环**（v2.1）：`/forgot-password` 生成一次性令牌存 Redis（`reset:token:{token}`，TTL 30 分钟）→ Spring Mail 发送；
  `/reset-password` 校验令牌 + BCrypt 更新 + 用后即删。
- **用户信息**：`GET /api/user/info`（含 roles 列表）。
- **init.sql**：15 张表全量对齐文档 4.2 + 种子数据（4 角色 / 9 权限 / admin 用户）。

### 第二阶段：用户系统（✅ 已完成并验证）

- **用户管理 CRUD**（`controller/admin/UserController.java`，ADMIN 权限）：
  - `GET /api/admin/users` 分页（用户名/昵称搜索 + 状态筛选，批量角色联查）
  - `GET /api/admin/users/{id}` 详情；`POST` 创建（可指定角色，空则默认 USER）
  - `PUT /api/admin/users/{id}` 修改（可重置密码、重设角色）；`DELETE` 删除（禁止删除当前登录账号）
- **角色权限管理**（`controller/admin/RoleController.java`，ADMIN 权限）：
  - `GET /api/admin/roles` 角色列表（含权限编码，供分配角色）
  - `POST/PUT/DELETE /api/admin/roles` 角色 CRUD（先清后插权限关联；内置角色与已分配角色禁止删除）
- **RBAC 强制**：SecurityConfig `/api/admin/**` 需 `ROLE_ADMIN`（HTTP 403 验证通过）
- **前端用户管理页**（`cms-frontend/src/views/user/index.vue`）：稿件台风格表格（USER-编号 + 状态色带）、
  搜索/筛选、新建/编辑弹窗（角色多选）、启用禁用、删除确认；路由 `/user` 已挂载

### 第三阶段：文章系统（✅ 已完成并验证）

- **文章 CRUD**（`controller/front/ArticleController.java` + `service/impl/ArticleServiceImpl.java`）：
  - 公开：`GET /api/articles`（分页，默认只看已发布；分类/关键词筛选）、`GET /api/articles/{id}`（详情）
  - 登录：`POST /api/articles`（创建，作者=当前用户）、`PUT/DELETE /api/articles/{id}`（**数据级权限：仅作者或 ADMIN**，文档 7.3）、`GET /api/articles/mine`
  - 置顶/推荐/摘要自动截取（正文前 200 字）/ SEO 字段
- **富文本 XSS 清洗**（`common/SanitizeUtil.java`，文档 7.8）：JSoup 白名单（p/strong/em/a/img/ul/ol/li/blockquote/code/pre/h1-h6/table 等），
  移除 script/事件属性/javascript: 协议
- **浏览量异步落库**（文档 7.7.1）：详情 Redis `INCR article:views:{id}`，展示 = DB 值 + Redis 增量；
  `job/ViewCountFlushJob.java` 每 5 分钟 GETDEL 增量批量落库 + **应用启动时补偿**
- **点赞防重**（文档 7.7）：`SADD article:liked:{id} {userId}` 幂等（重复点赞不重复计数），`SREM` 取消；
  首次/取消时同步更新 DB like_count（GREATEST 防负）
- **分类/标签**：公开 `GET /api/categories`、`GET /api/tags`（含文章数）；
  ADMIN `POST/PUT/DELETE`（有文章引用时禁止删除）
- **前端**（cms-frontend）：
  - 文章管理页（`views/article/list.vue`）：真实分页 + 状态筛选 + 稿件色带（草稿灰/发布绿/下架琥珀）+ 置顶/推荐标记 + 标签 chip + 发布/下架/删除
  - 文章编辑页（`views/article/edit.vue`）：wangEditor 富文本（隐藏图片/视频/附件菜单，媒体上传第五阶段接入 MinIO）、
    Markdown 导入（marked）、分类/标签多选、置顶/推荐、SEO 折叠面板、存草稿/发布
  - 分类管理页（`views/category/index.vue`）、标签管理页（`views/tag/index.vue`）

### 第四阶段：评论系统 + AI 机审（✅ 已完成并验证）

- **评论 CRUD**（`controller/front/CommentController.java` + `controller/admin/CommentController.java`，文档 5.1）：
  - 公开：`GET /api/articles/{id}/comments`（仅展示已通过评论）
  - 登录：`POST /api/articles/{id}/comments`（评论先以 status=0 入库 → **事务提交后**异步触发 AI 初审）、`DELETE /api/comments/{id}`（本人或 ADMIN）
  - ADMIN：`GET /api/admin/comments`（分页 + 状态/机审状态/内容筛选，含机审明细）、`PUT /api/admin/comments/{id}/audit`（人工复核覆盖机审结果）
  - 评论内容同样过 JSoup XSS 清洗（文档 7.8）；记录 IP
- **AI 机审管道**（文档 7.5）：
  - `service/ai/AiReviewService` 接口 + `RuleBasedAiReviewService` 规则实现（广告特征/辱骂词/违规词/重复刷屏 + 置信度加权）；
    **第七阶段由 Spring AI 真实模型替换实现，接口不变**
  - `CommentReviewService`：@Async 异步机审 → 落 `ai_review_record`（内容快照/结果/置信度，支持审计）→ 自动流转：
    normal 高置信 → 通过；ad/abuse/sensitive 置信度 ≥0.9 → 拒绝；否则/repeat/异常 → 转人工（不漏审）
  - 异步执行时机修复：`TransactionSynchronization afterCommit` 触发，避免异步线程读不到未提交评论

- **前端评论管理页**（`views/comment/index.vue`）：真实分页 + 审核状态/AI 机审状态筛选 +
  AI 机审展示（机审结果 + 置信度 + 原因）+ 人工复核（通过/拒绝）+ 删除

### 第五阶段：媒体管理 + MinIO（✅ 已完成并验证）

- **环境**：MinIO 已本地安装并运行（`D:\minio\minio.exe`，data 目录 `D:\minio\data`，API 9000 / 控制台 9001，root minioadmin/minioadmin）；bucket `cms` 由 `MinioConfig` 启动时自动创建并设置 PublicRead 直链策略（文档 9.3）
- **上传安全**（文档 7.8）：
  - 扩展名白名单：jpg/jpeg/png/gif/webp/pdf/docx/xlsx
  - MIME magic bytes 二次校验（伪造扩展名拦截，不以扩展名信任）
  - 大小限制：图片 ≤5MB、文档 ≤20MB
  - 服务端 UUID 随机文件名（防路径穿越与重名覆盖），按 yyyyMM 分目录
- **接口**（`controller/MediaController.java`，文档 5.1）：`POST /api/media/upload`（登录）、`GET /api/media`（分页+类型/关键字筛选，联查上传者昵称）、`DELETE /api/media/{id}`（本人或 ADMIN）
- **删除策略**（文档 7.10）：先删 DB 记录，再删 MinIO 对象；对象删除失败 WARN 留痕（孤儿文件风险提示）
- **前端**：`views/media/index.vue` 媒体库（上传/网格预览/复制链接/删除/分页/类型筛选）；文章编辑页 wangEditor 图片上传接入 `/api/media/upload`（插入 URL）+ 封面上传按钮

### 第六阶段：数据统计（✅ 已完成并验证）

- **访问日志**（文档 4.2.12 / 7.11）：`interceptor/VisitLogInterceptor` 记录 `/api/**` 请求（user/ip/UA/url/method/响应时间，`X-Forwarded-For` 取 IP，OPTIONS 跳过，未登录 userId=null），`VisitLogService.recordAsync` 经 `visitLogExecutor` 异步落库，写库失败不影响业务
- **统计接口**（`admin/StatsController.java`，文档 5.1，类级 `@PreAuthorize("hasRole('ADMIN')")`）：
  - `GET /api/stats/visits?days&granularity=day|month`：PV/UV 时间序列
  - `GET /api/stats/articles?topN`：文章状态分布 + 热门 TopN（view_count + Redis 未落库增量）
  - `GET /api/stats/users?days&granularity`：总数 + 按日/月新增 + 角色分布
  - `GET /api/stats/dashboard`：仪表盘概览（文章/用户/评论/今日 PV-UV/近 7 日趋势/待审稿件/最近发布）
- **口径实现要点**：PV=原始请求计数；UV=登录用户按 user_id 去重、游客按 ip 去重（`COUNT(DISTINCT CASE WHEN user_id IS NOT NULL THEN CONCAT('u',user_id) ELSE CONCAT('ip',ip_address) END)`）；热门文章浏览量 = DB `view_count` + Redis `article:views:{id}` 增量；`weekTrend` 缺失日期后端补零
- **前端**：`views/stats/index.vue`（ECharts：访问趋势 PV/UV 折线、文章状态环形、用户增长柱状、角色分布条形、热门 TopN 榜单；按日/按月 + 7/30/90 天切换）；`dashboard/index.vue` 由骨架示例改为接 `/api/stats/dashboard`（4 统计卡 + 近 7 日趋势图 + 待审稿件/最近发布真实列表）

## 验证记录（2026-09-29）

- `mvn compile` ✅（修复：MyBatis-Plus 3.5.9 分页插件独立模块补 mybatis-plus-jsqlparser）
- `mvn package -DskipTests` ✅（jar 已产出 target/cms-backend-0.1.0.jar）
- admin123 BCrypt `matches` 校验 ✅
- MySQL：root/20230115L 连接 ✅，init.sql 执行 ✅（15 表 / 4 角色 / 9 权限）
- **第一阶段认证联调 ✅**（8080 直连 + 5173 vite proxy 双通道）：
  登录/用户信息/刷新轮换/GETDEL 防重放(1005)/登出失效(1005)/Redis TTL 7 天
- **第二阶段用户系统联调 ✅**：
  - 用户列表/搜索/详情 ✅；创建（角色分配 EDITOR+AUTHOR 生效）✅
  - 新用户登录 ✅；修改（禁用 + 角色重设）✅；删除 + 删除后列表 ✅
  - 删除当前登录账号 → 400 保护 ✅；普通用户访问 /api/admin/** → HTTP 403 ✅
- **第三阶段文章系统联调 ✅**：
  - 分类/标签创建 + 公开列表 ✅；文章创建（标签绑定/置顶/推荐）✅
  - **XSS 清洗**：`<script>`、`javascript:`、`onclick` 全部移除 ✅
  - 公开列表默认只显示已发布（草稿隐藏）✅；mine 接口含草稿 ✅
  - 详情浏览量 INCR 递增（1→2→…）✅；启动补偿落库（DB=6、Redis 清空、展示=DB+增量）✅
  - 点赞幂等（重复点赞 likeCount 只 +1）✅；登录态 liked=true ✅；取消点赞 -1 ✅
  - 数据级权限：普通用户改他人文章 → code=403「只能操作自己的文章」✅
  - 修改（标题/标签重绑生效）✅；删除 → 详情 404 + 列表消失（逻辑删除）✅
- **第四阶段评论系统联调 ✅**：
  - 四类评论机审流转：正常 → 通过（status=1/aiStatus=1）✅；广告（微信+链接+优惠）→ 自动拒绝（status=2/aiStatus=3，置信度99）✅；
    辱骂/刷屏 → 转人工（status=0/aiStatus=2，置信度84/85）✅
  - ai_review_record 落库（aiResult/aiScore/aiReason 返回）✅；公开列表仅已通过 ✅
  - 人工复核：转人工评论 → 通过（status 覆盖，aiStatus 保留机审历史）✅
  - 未登录发表评论 → 401 ✅；他人评论删除 → 403「只能删除自己的评论」✅；本人删除 ✅
- **第五阶段媒体联调 ✅**：
  - 真实 PNG/PDF 上传成功（UUID 文件名 + URL 直链）✅；PNG 改名 .jpg 伪装 → 400「文件内容与扩展名不符」✅；.exe → 400 白名单拦截 ✅
  - 直链 PublicRead 访问 HTTP 200 ✅；未登录上传 → 401 ✅
  - 删除权限：他人文件 → 403「只能删除自己上传的文件」✅；本人删除 → 200 且 MinIO 对象同步清理（访问 404）✅
  - 文件列表联查昵称/大小/时间 ✅
- **第六阶段统计联调 ✅**：
  - 造访问数据（公开分页 + 详情 + 登录请求）→ 拦截器异步落库 ✅
  - `visits` 按日/按月：PV/UV 时间序列正确（UV 修复：`COUNT(DISTINCT user_id,ip)` 遇 user_id 全 NULL 返回 0 → 改为登录按 id/游客按 ip 去重）✅
  - 热门文章 TopN 浏览量 = DB 值 + Redis 增量（如 DB 10 + Redis 2 = 12）✅
  - `users` 总数/增长/角色分布 ✅；`dashboard` 文章/评论/今日 PV-UV/近 7 日补零趋势/待审/最近发布 ✅
  - 权限：非 ADMIN → 403「无权限访问」，未登录 → 401 ✅
  - 前端 stats 页四个 ECharts 图表 + TopN 榜单渲染真实数据，按日/按月切换正常；dashboard 真实数据（浏览器实测）✅
- **第七阶段 Spring AI 智能能力联调 ✅**（真实模型 qwen2.5:3b @ Ollama，Spring AI 1.0.0）：
  - **评论 AI 初审真实模型替换**：发表评论异步机审，`ai_review_record.model = qwen2.5:3b`（旧数据 rule-based-v1）；
    广告评论 → `ad` 置信度 0.99 自动拒绝（status=2/aiStatus=3）✅；正常评论 → `normal` 置信度 0.04 放行（status=1/aiStatus=1）✅；
    模型异常/超时自动降级规则机审并标注「模型降级」（未触发场景，逻辑保留）✅
  - **AI 写作助手**（登录即可，SSE）：`POST /api/ai/write/draft|continue|polish` 逐字流式返回（浏览器实测逐字渲染全文 2505 字）✅；
    `POST /api/ai/article/enhance` 摘要+SEO 四项（summary/seoTitle/seoDescription/seoKeywords）解析回填 ✅
  - **生成记录落库与成本审计**：`ai_generation_record` status/result/model 完整落库（status=1 成功）✅；
    Ollama 不返回 usage 元数据 → tokens_used 记 0（代码注释已注明限制，DashScope 接入后自动填充）✅
  - **限流**：每分钟 5 次（Redis INCR + 60s 过期，`count > 5` 抛 2002），同分钟第 6 次调用被拦 ✅
  - **前端 AI 面板**：文章编辑页 AI 写作助手抽屉（4 Tab：草稿/续写/润色/摘要+SEO，SSE 逐字渲染 + 结果插入编辑器 + 增强一键回填）；
    AI 中心页 `ai/generations` 生成记录表格（类型/状态色带/模型/Tokens/查看详情），浏览器实测渲染真实数据 ✅
  - **已知限制**：① Ollama 无 usage 统计（tokens=0，字段保留）；② DashScope starter 不在 Maven Central，当前仅 Ollama 生效（`spring.ai.dashscope.api-key=${DASHSCOPE_API_KEY}` 已配置留位，拿到 Key 后可接入自动切换）
- 遗留说明：Windows JDBC 连 `localhost` 会走 IPv6 挂起，datasource url 已固定为 `127.0.0.1`

## 环境就绪（当前机器状态）

| 依赖 | 状态 | 说明 |
|---|---|---|
| JDK 21 + Maven 3.9.14 | ✅ | 已装（D:\jdk） |
| MySQL 8.0.39 | ✅ | 已装（D:\MySQL），密码已对齐（root/20230115L），init.sql 已执行（15 表 + 种子数据核验通过） |
| Redis 7 | ✅ | 部署于 WSL（Linux），Windows 侧 localhost:6379 转发可达 |
| MinIO | ✅ | 已装（D:\minio\minio.exe，控制台 9001，bucket cms PublicRead） |
| Ollama | ✅ | 已装（WSL，http://localhost:11434，qwen2.5:3b 已加载 2064MB；deepseek-r1:1.5b 备用） |
| Docker | ❌ | 未安装（docker-compose 第八阶段用） |

> 联调状态：后端 8080 运行中，admin/admin123 真实登录链路全通（见下方验证记录）。

## 快速开始（环境就绪后）

```powershell
# 1. 建库建表 + 种子数据
mysql -uroot -p < src/main/resources/sql/init.sql

# 2. 启动（默认 dev profile，localhost:3306/6379）
mvn spring-boot:run

# 3. API 文档
http://localhost:8080/doc.html

# 4. 登录联调（与前端 mock 契约一致，见 KEY 契约）
POST /api/auth/login  {"username":"admin","password":"admin123"}
```

## 前端联调契约（mock-server 已按此实现，前后端可无缝切换）

| 接口 | 说明 |
|---|---|
| POST /api/auth/login | 返回 accessToken + refreshToken + tokenType + expiresIn |
| POST /api/auth/refresh | 携带 refreshToken，返回新令牌对（后端轮换，mock 复用） |
| POST /api/auth/logout | 注销 RefreshToken |
| GET /api/user/info | 当前用户信息（含 roles） |

前端 `.env.development` 已支持 `VITE_DEMO_MODE=false` 直连真实后端（proxy 5173 → 8080）。

## 后续阶段占位

- 第七阶段：Spring AI（✅ 已完成：评论机审真实模型替换、AI 写作助手 SSE、摘要 SEO、生成记录与限流；剩余 RAG/向量库为 P1 不在本期）
- 接口级权限：`@PreAuthorize("hasAuthority('article:create')")` / ADMIN 数据级权限（文档 7.3）
