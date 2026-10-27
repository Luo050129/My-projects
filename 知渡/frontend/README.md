# 知渡前端

知渡社区的前端，Vue 3 + Vite。**不引入 UI 组件库**，样式手写，依赖只有 `vue` 和 `vue-router`。

---

## 快速开始

### 1. 先起后端

```bash
cd ..                       # 回到知渡根目录
mvn -q -DskipTests package
java -jar target/zhidu-community.jar --spring.profiles.active=local --server.port=8088
```

> Windows 上注意用 JDK 21 的完整路径，PATH 里那个可能是 Java 8。

### 2. 再起前端

```bash
cd frontend
npm install
npm run dev
```

打开 http://localhost:5173

演示账号：`admin / admin123`、`alice / alice123`、`bob / bob123`

---

## 技术栈

| 层 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | Vue 3（Composition API + `<script setup>`） | 只做视图，不引入状态管理库 |
| 路由 | Vue Router 4 | 路由守卫处理登录态 |
| 构建 | Vite 6 | 开发服务器自带 `/api` 代理 |
| 请求 | 原生 `fetch` | 统一封装在 `src/api.js` |
| 状态 | `reactive` + localStorage | 只需要存 token 和用户信息 |

---

## 目录结构

```
frontend/
├── index.html
├── vite.config.js          # dev server + /api 代理
├── package.json
└── src/
    ├── main.js
    ├── App.vue
    ├── style.css           # 全局样式与设计变量
    ├── auth.js             # 登录态（token + 用户信息）
    ├── api.js              # 所有接口封装
    ├── router.js           # 路由与守卫
    ├── utils.js            # 时间格式化
    ├── components/
    │   └── NavBar.vue      # 顶部导航 + 未读通知角标
    └── views/
        ├── LoginView.vue        # 登录 / 注册
        ├── PostListView.vue     # 帖子列表（搜索 + 分页）
        ├── PostDetailView.vue   # 详情（正文 + 点赞 + 评论 + AI 摘要）
        ├── PostEditorView.vue   # 发帖 / 编辑
        └── NotificationsView.vue # 通知中心
```

---

## 页面功能

| 页面 | 路由 | 能力 |
| --- | --- | --- |
| 登录 / 注册 | `/login` | 登录、注册后自动登录；登录后访问此页会跳回列表 |
| 帖子列表 | `/posts` | 关键字搜索、翻页、显示浏览/点赞/评论数、AI 摘要标记 |
| 发帖 | `/posts/new` | 标题 + 正文 + 标签，正文有字数提示（上限 8000） |
| 帖子详情 | `/posts/:id` | 显示正文与标签；点赞；作者可编辑/删除/触发 AI 摘要；评论区可发表评论 |
| 通知中心 | `/notifications` | 未读高亮、单条标记已读、一键全部已读、跳转到关联帖子 |

导航栏会实时显示未读通知数（路由切换时刷新）。

---

## 与后端的约定

- **接口前缀** `/api`，开发时由 Vite 代理到 `http://127.0.0.1:8088`，前端代码里不出现硬编码地址。
- **响应结构** `{ code, message, data }`，`code === 0` 视为成功；非 0 抛出 `message`。
- **认证** JWT 放在 `Authorization: Bearer <token>`；收到 401 会清空登录态并提示重新登录。
- **正文字段** `PostVO.content` 只在详情接口返回，列表接口为 `null`（避免列表响应体过大）。

> 后端 `SecurityConfig` 里 CORS 允许任意来源（`allowedOriginPatterns("*")`），所以即使不用代理、直接跨域调用也能通。代理的作用是让前端代码保持同源相对路径。

---

## 构建与部署

```bash
npm run build       # 产物在 dist/
```

两种部署方式：

**A. 交给后端托管（简单）**

把 `dist/` 里的文件拷到后端 `src/main/resources/static/`，重启后端后直接访问 `http://127.0.0.1:8088/`。
好处是同源、无跨域、只需一个端口。

**B. 独立托管（Nginx 等）**

注意两点：SPA 需要把未匹配的路径回退到 `index.html`，以及把 `/api` 反向代理到后端。

---

## 已知限制

| # | 限制 | 说明 |
| --- | --- | --- |
| 1 | 没有状态管理库 | 登录态放在一个 `reactive` 对象里，规模再大应换 Pinia |
| 2 | 没有表单校验库 | 只做了"非空"级别的前端校验，长度/格式依赖后端 |
| 3 | 通知未读数是拉取式 | 路由变化时请求一次，没有 WebSocket 推送 |
| 4 | 不支持富文本 | 正文按纯文本渲染（`white-space: pre-wrap`），没有 Markdown 或编辑器 |
| 5 | 没有图片上传 | 后端暂未提供上传接口 |
| 6 | 无单元测试 | 组件逻辑较薄，主要靠手动走查 |
