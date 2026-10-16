# CMS 内容管理系统 · 前端设计系统 v1.0

> 设计语言：**编辑部 Editorial**（稿件工作台）
> 依据：Anthropic `frontend-design` Skill 设计原则（配色/字体/版式/签名元素四槽位 + 两遍法 + 反 AI 痕迹清单）
> 适用范围：本项目前端所有页面（Vue3 + Element Plus + ECharts + TinyMCE），后续页面开发一律按本规范执行
> 版本：v1.0（2026-09-29）

---

## 0. 一句话定位

把 CMS 做成一个**编辑部稿件工作台**：纸感底色、墨色文字、编辑批注色、稿件状态色带。它专业、安静、有内容管理场景独有的记忆点，不落 AI 默认模板的任何组合。

## 1. 设计语言（四槽位）

| 槽位 | 取值 | 说明 |
|---|---|---|
| **World（世界）** | 编辑部 / 稿件工作台 | 编辑、审稿、发布内容的工作流；页面是"处理稿件的台面"而非"展示卡片的橱窗" |
| **Materials（材质）** | 纸张白、墨色、排版网格、编辑批注 | 页面底色像纸张而非屏幕灰；边框像排版网格线而非卡片描边；警告/待办用编辑批注的红笔色 |
| **Palette（配色）** | 见 §2 | 墨绿主色 + 暖纸白底 + 低饱和语义色，避开奶油+陶土、紫蓝渐变、近黑+荧光 |
| **Signature（签名元素）** | 稿件状态色带 + 稿件编号 | 文章列表左侧 3px 状态色带（草稿灰/审核琥珀/发布绿/驳回红）+ 每篇稿件编号。只在"内容管理"场景成立，换到任何其他项目都不能复用 |

## 2. 色彩令牌

### 2.1 基础中性色（色板底）

| 令牌 | 值 | 用途 |
|---|---|---|
| `--bg-page` | `#F6F4EF` | 页面底色（暖纸白，非冷灰） |
| `--bg-surface` | `#FFFFFF` | 卡片 / 容器 / 表格底色 |
| `--bg-subtle` | `#FAF9F5` | 表头、次级区块底 |
| `--text-primary` | `#23262B` | 主文本（墨黑偏暖，禁纯黑 #0B0B0B） |
| `--text-secondary` | `#5E6570` | 次文本 / 说明 |
| `--text-tertiary` | `#9AA1AB` | 辅助信息 / 占位符（不低于此深度） |
| `--border-default` | `#E4E0D8` | 默认边框（暖灰，禁冷灰 #E4E7ED） |
| `--border-strong` | `#CFCABF` | 强化边框 / 分隔 |

### 2.2 品牌与语义色

| 令牌 | 值 | 用途 |
|---|---|---|
| `--brand-primary` | `#1F6B5A` | 主品牌色（墨绿，覆盖 Element Plus 默认蓝 #409EFF） |
| `--brand-hover` | `#1A5A4C` | 主色 hover |
| `--brand-active` | `#164D41` | 主色 active |
| `--brand-light` | `#E7F0ED` | 主色浅底（选中行、选中菜单项） |
| `--accent-red` | `#B4543B` | 编辑批注红（仅语义：待办/警告签名，禁大面积） |
| `--success` | `#3E7C4F` | 成功 / 已发布 |
| `--warning` | `#C98A2D` | 警告 / 审核中 |
| `--danger` | `#C0453C` | 错误 / 驳回 |
| `--info` | `#5E6570` | 中性提示 |

> 约束：语义色一律低饱和，禁荧光绿/高饱和红；语义色不作大面积底色（对比度 ≥ 4.5:1）。

### 2.3 稿件状态色带（签名元素专用）

| 状态 | 色带值 | 语义 |
|---|---|---|
| 草稿 | `#9AA1AB` | 灰 |
| 审核中 | `#C98A2D` | 琥珀 |
| 已发布 | `#3E7C4F` | 绿 |
| 已驳回 | `#C0453C` | 红 |

色带形态：列表项左侧 `3px` 实心竖条，圆角 `1px`。仅文章列表 / 稿件相关卡片使用，其他页面不得挪用。

## 3. 字体令牌

| 令牌 | 字体栈 | 用途 |
|---|---|---|
| `--font-ui` | `'PingFang SC','HarmonyOS Sans SC','Microsoft YaHei UI','Segoe UI',sans-serif` | 全部界面文字（正文、按钮、表格） |
| `--font-display` | `'Noto Serif SC','Source Han Serif SC','Songti SC',serif` | **仅签名场景**：登录页主标题、仪表盘数字大标题、空态标题。全站不超过 3 处 |
| `--font-mono` | `'JetBrains Mono','SFMono-Regular',Consolas,monospace` | 统计数字、文章 ID、时间戳等数据类文字（tabular） |

### 字号与行高（8px 级）

| 级别 | 字号 | 字重 | 行高 | 场景 |
|---|---|---|---|---|
| display | 32px / 24px | 600 / 600 | 1.3 | 仪表盘数字标题、登录页标题（配合 font-display） |
| title | 20px / 16px | 600 | 1.4 | 页面标题、卡片标题 |
| body | 14px | 400 | 1.6 | 正文、表格内容 |
| small | 13px | 400 | 1.5 | 表头、标签、说明 |
| caption | 12px | 400 | 1.5 | 辅助信息、时间戳 |

约束：正文行宽 ≤ 80 字符；所有数字默认 `font-variant-numeric: tabular-nums`。

## 4. 间距 / 圆角 / 阴影体系

### 4.1 间距（8px 基准）

`4 / 8 / 12 / 16 / 24 / 32 / 48 / 64`

- 页面内容区 padding：`24px`（≤ 1440px 屏），`32px`（宽屏）
- 卡片内边距：`20px`（常规）`24px`（仪表盘大卡）
- 组件间默认间距：`16px`

### 4.2 圆角（分级，禁止统一圆角）

| 令牌 | 值 | 用途 |
|---|---|---|
| `--radius-none` | `0` | 表格行、列表项、输入框内嵌元素（**默认不圆角**） |
| `--radius-sm` | `4px` | 输入框、按钮、标签、下拉 |
| `--radius-md` | `8px` | 卡片、弹窗、抽屉 |
| `--radius-lg` | `12px` | 大卡片、空态插图容器 |

> 反模板关键：**圆角必须分等级**。表格行和列表项 0 圆角，只有卡片才有圆角——杜绝"所有元素一个圆角"的 SaaS 卡片套件感。

### 4.3 阴影（默认无阴影）

| 令牌 | 值 | 用途 |
|---|---|---|
| `--shadow-none` | `none` | **默认**：表格、列表、常规卡片一律无阴影（用 1px 暖灰边框区分） |
| `--shadow-sm` | `0 2px 8px rgba(35,38,43,0.06)` | 卡片 hover、下拉面板 |
| `--shadow-md` | `0 8px 24px rgba(35,38,43,0.10)` | 弹窗、抽屉 |

> 禁止：所有卡片同款 `rgba(0,0,0,0.1)` 灰阴影；禁止渐变水彩装饰底色。

## 5. 版式原则

1. **左对齐优先**：正文、卡片标题、列表一律左对齐；表格数字列右对齐。禁止大段居中排版。
2. **网格**：内容区 12 栅格，8px 基准；卡片按栅格排布，禁等宽等大卡片阵列。
3. **信息层级靠字重和留白**，不靠加框加底色。
4. **编号标记（01/02/03）只在内容真是序列时使用**（如"发布流程三步"），列表/卡片禁用。
5. **页面结构**：侧边栏（`240px` 浅底）+ 顶栏（`56px`）+ 内容区（最大宽度 `1280px` 居中约束，实际左对齐）。侧边栏禁深色底（近黑侧栏是 AI 模板高频特征）。
6. 空态 = 行动邀请：一句说明 + 主操作按钮 + 浅底插图，不渲染情绪。

## 6. 签名元素：稿件台

- **形态**：文章管理页列表每行左侧 `3px` 状态色带 + 行首稿件编号（如 `#0247`，font-mono 12px 灰）。
- **唯一性**：此手法只在 CMS 文章场景出现；仪表盘"待审稿件"卡片复用色带，其余页面不出现。
- **克制**：全站只保留这一个签名手法，其他元素保持安静规范（"把大胆花在一个地方"）。

## 7. 组件规范（Element Plus 覆盖）

### 7.1 按钮
- 主按钮：`--brand-primary` 实心、白字、`4px` 圆角、无阴影、无渐变；hover 用 `--brand-hover`
- 次按钮：白底 + `--border-strong` 描边 + 墨黑文字
- 文字按钮：无边框无底色，仅墨绿文字
- 危险操作：`--danger` 实心；确认弹窗按钮不用渐变
- **禁止**：按钮文字加 "→"、全大写按钮文案（用"保存更改"而非"SUBMIT"）

### 7.2 卡片
- 白底 `--bg-surface` + `1px solid --border-default` + `8px` 圆角 + `--shadow-none`
- hover 时提升到 `--shadow-sm`（仅仪表盘可交互卡片）
- 卡片标题 16px/600，下方可加细分隔线 `1px solid --border-default`
- **禁止**：卡片渐变底、玻璃拟态、发光光晕

### 7.3 表格
- 无外框卡片包裹（白底 + 1px 暖灰边 + 8px 圆角）
- 表头：`--bg-subtle` 底、13px/600、左对齐（数字列右对齐）
- 行 hover：`--brand-light` 浅墨绿底；行之间用细分割线（禁斑马纹）
- 状态列用稿件色带（见 §6）；操作列文字按钮
- 分页组件贴表格底部，无边框装饰

### 7.4 表单
- 标签：13px 左对齐，与控件间距 8px；必填项用批注红 `*`（`--accent-red`）
- 输入框：白底 + `1px --border-default` + `4px` 圆角；聚焦时 `2px solid --brand-primary`（覆盖 Element Plus 默认蓝）
- 校验错误：`--danger` 文字 + 细线提示
- 编辑器（TinyMCE）：包裹在无阴影白卡内，工具栏去渐变、扁平图标

### 7.5 导航
- 侧边栏：`--bg-page` 底、无边框、无阴影；菜单项左对齐 14px
- 选中项：`--brand-light` 底 + 左侧 3px 墨绿竖条（与稿件色带同一语言）；hover 仅浅灰底
- 顶栏：白底 + 底部 1px 暖灰线；面包屑 13px

### 7.6 标签（Tag）
- 低饱和浅底 + 深色文字：`--brand-light`/墨绿字（审核）、琥珀浅底/琥珀深字（待办）、红浅底/红深字（驳回）
- 圆角 `4px`，禁渐变、禁外发光
- 图标 + 文字并排，图标 12px

### 7.7 图表（ECharts）
- 统一 `--brand-primary` 为主系列色，辅助系列用低饱和中性灰
- 网格线 `#E4E0D8` 细线；坐标轴文字 12px/`--text-secondary`
- 禁用：默认蓝 `#409EFF`、紫蓝渐变、发光光晕、3D 效果

## 8. 动效规范

只保留 **2 处**编排时刻，其余一律不动：

1. **页面切换**：`fade` 200ms ease（路由进入淡入），全局统一
2. **仪表盘数字入场**：一次编排（数字递增 + 标题浮现 300ms，各卡片 `animation-delay` 交错 60ms）

**禁止**：每张卡片 hover 动画、每区块 fade+slide-up、持续旋转/呼吸/漂浮装饰动画、无意义 hover 位移。尊重 `prefers-reduced-motion`。

## 9. 反 AI 痕迹检查清单（每页交付前自查）

| # | 检查项 | 标准 |
|---|---|---|
| 1 | 紫蓝渐变 / 发光光晕 / 玻璃拟态 | 全站禁止 |
| 2 | 奶油底 + 衬线大字 + 陶土强调的组合 | 底色是暖纸白但强调是墨绿，不得出现陶土/砖红大面积 |
| 3 | 近黑背景 + 单一荧光色 | 侧边栏/顶栏禁深色底 |
| 4 | 统一圆角（所有元素同一圆角） | 表格行 0、按钮 4、卡片 8，必须分级 |
| 5 | 所有卡片同款灰阴影 | 默认无阴影，边框区分 |
| 6 | 标题上方全大写眉标 | 全站禁止 |
| 7 | "A · B · C" 中点分隔元信息 | 用普通间隔或换行 |
| 8 | 按钮文字加 "→" | 禁止 |
| 9 | 纯黑 #0B0B0B 代替墨黑 | 统一 `--text-primary` |
| 10 | 编号 01/02/03 装饰 | 仅真实序列使用 |
| 11 | Element Plus 默认蓝 #409EFF | 全局覆盖为墨绿 |
| 12 | 每个区块 fade+slide-up 动效 | 只保留 §8 两处编排 |

## 10. Element Plus 落地实现

### 10.1 全局 CSS 变量覆盖（`styles/design-tokens.css`）

```css
:root {
  /* 品牌色 */
  --el-color-primary: #1F6B5A;
  --el-color-primary-light-3: #4C8B7D;
  --el-color-primary-light-5: #85B3A9;
  --el-color-primary-light-7: #BED4CF;
  --el-color-primary-light-8: #E7F0ED;
  --el-color-primary-dark-2: #1A5A4C;
  /* 语义 */
  --el-color-success: #3E7C4F;
  --el-color-warning: #C98A2D;
  --el-color-danger: #C0453C;
  --el-color-error: #C0453C;
  --el-color-info: #5E6570;
  /* 中性 */
  --el-text-color-primary: #23262B;
  --el-text-color-regular: #5E6570;
  --el-text-color-secondary: #9AA1AB;
  --el-border-color: #E4E0D8;
  --el-border-color-light: #E4E0D8;
  --el-border-color-lighter: #EDEAE3;
  --el-fill-color-blank: #FFFFFF;
  --el-fill-color-light: #FAF9F5;
  --el-bg-color-page: #F6F4EF;
  /* 几何 */
  --el-border-radius-base: 4px;
  --el-border-radius-small: 4px;
  --el-border-radius-round: 8px;
  /* 阴影：默认归零，仅弹窗保留 */
  --el-box-shadow: none;
  --el-box-shadow-light: none;
  --el-box-shadow-lighter: none;
  --el-box-shadow-dark: 0 8px 24px rgba(35,38,43,0.10);
  /* 字体 */
  --el-font-family: 'PingFang SC','HarmonyOS Sans SC','Microsoft YaHei UI','Segoe UI',sans-serif;
}
```

### 10.2 组件级微调（`styles/editorial.scss`）

- 表格：表头底色 `#FAF9F5`、行 hover `#E7F0ED`
- 侧边栏菜单选中：`background: #E7F0ED; border-left: 3px solid #1F6B5A`
- 输入框聚焦：`box-shadow: 0 0 0 2px rgba(31,107,90,0.25)`（替代默认蓝）
- 文章列表状态色带：`.article-row::before { content:''; width:3px; border-radius:1px; background: var(--status-color); }`
- 全局数字：`font-variant-numeric: tabular-nums`

### 10.3 字体加载方案

- UI 字体：系统栈，零加载成本
- `font-display`（Noto Serif SC）：仅登录页/仪表盘 3 处使用，npm `@fontsource/noto-serif-sc` 或按需引入子集，未加载完成时回退 `Songti SC` / 衬线系统栈
- 禁用外链字体 CDN（生产环境字体走项目内静态资源）

## 11. 页面应用示例（落地速查）

| 页面 | 关键处理 |
|---|---|
| **登录页** | 左对齐大标题用 `font-display`（如"内容管理，像编辑一份刊物"）+ 墨绿主按钮；右侧留白，禁居中卡片 + 紫蓝渐变背景 |
| **仪表盘** | 顶部 4 个统计卡：大数字 `font-display` 32px + tabular-nums，卡片 8px 圆角无阴影；"待审稿件"卡用琥珀色带 |
| **文章管理** | 稿件台：状态色带 + 稿件编号；表格行 hover 墨绿浅底；发布/驳回操作用语义色文字按钮 |
| **文章编辑** | TinyMCE 白卡无阴影；保存按钮墨绿实心；工具栏扁平化 |
| **评论管理** | 表格 + 状态标签（待审琥珀/通过绿/驳回红），操作列文字按钮 |
| **AI 写作助手** | 生成结果卡片用 `--brand-light` 浅墨绿底区分"机器生成"；禁用紫色渐变"AI 感"装饰 |
| **空态页** | 浅底插图容器 12px 圆角 + 一句行动指引 + 主操作按钮 |

---

## 附：与 v2.1 需求文档的关系

本规范独立成文，作为前端开发的唯一视觉依据；后续如需要可并入 v2.1 附录（新增"附录 E 前端设计系统"章节），页面开发时按 §7 组件规范逐页落地并对照 §9 清单自查。
