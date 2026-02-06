# Frontend（Nuxt）协作指引

## 1) 适用范围

- 作用于 `frontend_nuxt/` 目录及其子目录。
- 本文件仅覆盖前端范围；跨服务规则仍遵循根 `AGENTS.md`。

## 2) 代码组织约定

- `pages/`：路由页面与页面级数据获取。
- `components/`：可复用视图组件。
- `composables/`：状态与行为复用（如 WebSocket、倒计时）。
- `plugins/`：运行时插件（鉴权 fetch、主题、第三方库注入）。
- `utils/`：纯工具函数（时间、鉴权 token、平台适配）。
- `assets/`、`public/`：静态资源与样式。

## 3) 前端修改规则

- 优先保持现有交互和视觉风格一致，不做无关 UI 重构。
- 接口字段变更时，先更新调用点，再统一处理回退逻辑与空值分支。
- SSR 与客户端代码分离：
  - 涉及 `window`、`localStorage`、WebSocket 的逻辑只在 client 侧运行。
- 与鉴权相关的 401 处理，保持与 `plugins/auth-fetch.client.ts` 行为一致。

## 4) 环境变量与运行时配置

- 统一通过 `nuxt.config.ts` 的 `runtimeConfig.public` 读取。
- 关键键位保持一致：
  - `NUXT_PUBLIC_API_BASE_URL`
  - `NUXT_PUBLIC_WEBSOCKET_URL`
  - `NUXT_PUBLIC_WEBSITE_BASE_URL`
- 变量改动需同步根目录 `.env.example` 与文档说明。

## 5) 实时消息链路注意事项

- WebSocket 入口：
  - `composables/useWebSocket.js`
- 若改订阅目标（`/topic/...`、`/user/...`），必须与后端推送目的地保持一致。
- 重连与重订阅逻辑不可被破坏；避免引入重复订阅和泄漏。

## 6) 构建与验证

- 标准验证：`npm run build`
- 本地联调：`npm run dev`
- 涉及 WebSocket/通知的改动，建议至少手工验证：
  - 登录后连接建立
  - 收到消息时 UI 状态更新
  - 断线重连后仍可订阅

## 7) 输出要求

- 标注影响页面/组件路径。
- 标注是否引入 API 字段兼容处理。
- 标注是否需要后端或 WebSocket 服务配合发布。
