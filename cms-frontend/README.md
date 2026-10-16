# cms-frontend · CMS 内容管理系统前端骨架

> 设计系统：**编辑部 Editorial**（见项目根目录 `CMS前端设计系统_v1.0.md`）
> 技术栈：Vue 3 + TypeScript + Vite 5 + Element Plus + Pinia + Vue Router 4 + Axios

## 启动

```bash
npm install        # 安装依赖（已执行）
npm run dev        # 开发服务器 → http://localhost:5173
npm run build      # 类型检查 + 生产构建（已验证通过）
```

## 演示模式

`.env.development` 中 `VITE_DEMO_MODE`：
- `true`：后端未就绪时，登录页输入任意账号密码即可进入后台（使用假令牌 + 演示用户，不请求后端）
- `false`（默认）：走真实 `/api/auth/login` 链路。**联调用 mock 后端**（见下），后端开发完成后替换为真实 Spring Boot 服务（8080 端口不变）

## Mock 后端联调

`mock-server/server.mjs`（零依赖 Node 服务，监听 8080），实现文档 §5.1 认证接口契约：

```bash
npm run mock          # 启动 mock 后端（账号 admin / admin123）
```

已实现接口：`POST /api/auth/login`（返回双令牌）、`POST /api/auth/refresh`、`POST /api/auth/logout`、`GET /api/user/info`（含无效 Token 401 场景）。
前端链路：登录页 → `src/utils/request.ts`（401 自动刷新 AccessToken 并重试）→ Vite proxy（5173 → 8080）。已联调验证通过。

## 已实现页面（骨架阶段）

| 路由 | 页面 | 设计系统应用 |
|---|---|---|
| `/login` | 登录页 | 左对齐宋体大标题、白卡无阴影表单、无紫蓝渐变、签名元素微示 |
| `/dashboard` | 仪表盘 | 大数字 font-display + tabular、无阴影统计卡、待审稿件色带 |
| `/article/list` | 文章管理 | **稿件台签名元素**：行左侧 3px 状态色带 + 稿件编号 + 状态标签 |
| `/comment` | 评论管理 | **AI 机审状态展示**：normal/ad/abuse/sensitive/repeat 分类标签 + 置信度 + 人工复核（通过/拒绝/改回待审） |
| `/article/edit/:id` | 文章编辑 | 编辑器骨架（后续集成 TinyMCE 6） |
| 其余路由 | 占位页 | 设计系统空态（行动邀请式） |

## 目录结构（与需求文档 v2.1 §6.2 对齐）

```
cms-frontend/
├── package.json / vite.config.ts / tsconfig*.json / index.html
├── .env.development / .env.production
└── src/
    ├── main.ts                  # 入口（Element Plus 全量引入 + 中文 locale + 全局样式）
    ├── App.vue
    ├── api/auth.ts              # 认证接口（login/register/logout/refresh/forgot/reset）
    ├── assets/styles/
    │   ├── variables.scss       # 设计令牌（设计系统 §2-§4 唯一来源）
    │   └── index.scss           # Element Plus 覆盖 + 签名元素全局类（§7/§10）
    ├── components/Layout/       # 管理后台布局（index/Header/Sidebar/Footer）
    ├── router/index.ts          # 路由表
    ├── router/permission.ts     # 登录守卫
    ├── store/                   # user.ts（登录/登出）+ app.ts（侧栏折叠）
    ├── types/                   # api.d.ts / user.d.ts
    ├── utils/                   # request.ts（401 自动刷新重试）+ auth.ts + storage.ts
    └── views/                   # login / dashboard / article / placeholder
```

## 设计系统落地要点（对照 CMS前端设计系统_v1.0.md）

- **Element Plus 全局覆盖**：默认蓝 `#409EFF` → 墨绿 `#1F6B5A`（`index.scss :root`），聚焦环、表格 hover、菜单选中全部换色
- **圆角分级**：列表 0 / 按钮 4 / 卡片 8 / 大卡 12
- **无同款灰阴影**：默认 `box-shadow: none`，仅弹窗保留 `shadow-md`
- **签名元素**：`.article-row` 行级 3px 状态色带（`status-draft/reviewing/published/rejected`）
- **动效克制**：仅页面切换 fade 200ms（Layout/index.vue）
- **待办**：仪表盘/文章列表当前为 mock 数据，接入后端后由 `/api/admin/stats`、`/api/admin/articles` 替换；Element Plus 可后续切换按需引入（unplugin-vue-components）以消除 1.2MB 全量 chunk 警告

## 开发检查（每页交付前自查）

对照设计系统文档 §9 反 AI 痕迹清单：无紫蓝渐变 / 无统一圆角 / 无同款灰阴影 / 无全大写眉标 / 无"A·B·C" / 无按钮"→" / 无近黑侧栏 / 无默认蓝残留。
