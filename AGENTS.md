# OpenIsle 协作指引（根目录）

## 1) 适用范围与优先级

- 本文件作用于整个仓库。
- 若子目录存在 `AGENTS.md`，以“最近目录”的规则为准（就近覆盖）。
- 没有子目录规则时，回退到本文件。

## 2) 仓库地图（高频模块）

- `backend/`：Spring Boot 主后端（JPA、Redis、RabbitMQ、OpenSearch、OpenAPI）。
- `frontend_nuxt/`：Nuxt 3 前端（SSR + WebSocket + OAuth 回调页）。
- `websocket_service/`：独立 WebSocket/STOMP 服务（消费 RabbitMQ 通知并推送）。
- `mcp/`：Python MCP Server（封装搜索、发帖、回复、通知等工具）。
- `docs/`：Fumadocs 文档站（含 OpenAPI 生成）。
- `deploy/`：生产/预发部署脚本。
- `bots/`：定时 Bot 脚本（由 GitHub Actions 调度）。

## 3) 全局工作原则

- 小步修改：仅改当前任务相关文件，不做无关重构。
- 先对齐契约，再改实现：优先保证 API、消息结构、环境变量名称稳定。
- 变更需可验证：提交前给出最小可执行验证命令与结果。
- 安全优先：禁止提交密钥、令牌、生产凭证。

## 4) 任务路由（先判定影响面）

- 仅后端业务逻辑：进入 `backend/AGENTS.md`。
- 仅前端页面交互：进入 `frontend_nuxt/AGENTS.md`。
- 实时消息/在线通知：同时看 `backend/` + `websocket_service/` + `frontend_nuxt/`。
- API/DTO/鉴权变更：至少看 `backend/` + `mcp/` + `docs/`。
- 部署链路变更：进入 `deploy/AGENTS.md`，并附风险与回滚方案。

## 5) 跨服务不变量（必须遵守）

- 环境变量以根目录 `.env.example` 为统一基线；变量改名需同步消费端。
- 鉴权链路一致：
  - 后端 JWT：`backend/src/main/java/com/openisle/config/SecurityConfig.java`
  - WebSocket JWT：`websocket_service/src/main/java/com/openisle/websocket/config/WebSocketAuthInterceptor.java`
- RabbitMQ 分片一致（16 个十六进制分片 + 兼容遗留队列）：
  - `backend/src/main/java/com/openisle/config/RabbitMQConfig.java`
  - `backend/src/main/java/com/openisle/config/ShardingStrategy.java`
  - `websocket_service/src/main/java/com/openisle/websocket/listener/NotificationListener.java`
- API 契约变更要同步：
  - MCP schema/tool：`mcp/src/openisle_mcp/schemas.py`、`mcp/src/openisle_mcp/server.py`
  - 文档生成：`docs/scripts/generate-docs.ts`、`docs/lib/openapi.ts`

## 6) 最小验证矩阵（按改动类型）

- 后端改动：`cd backend && mvn test`
- 前端改动：`cd frontend_nuxt && npm run build`
- WebSocket 服务改动：`cd websocket_service && mvn test`（无测试可退化为 `mvn -DskipTests compile`）
- MCP 改动：`cd mcp && python -m pip install -e .`
- Docs/OpenAPI 改动：`cd docs && bun run generate && bun run build`
- 部署脚本改动：`bash -n deploy/deploy.sh && bash -n deploy/deploy_staging.sh`

## 7) 交付清单（每次任务输出建议）

- 改了什么：按文件列出核心变更点。
- 为什么改：说明触发原因/缺陷点/一致性要求。
- 如何验证：给出实际执行命令与结果摘要。
- 风险与后续：列出剩余风险、可选回归点、回滚建议。

## 8) 禁止事项

- 不提交 `.env`、密钥、生产 token。
- 不在未明确授权下执行破坏性命令（如大范围删除、强制重置）。
- 不在无关文件中进行格式化/重排以“顺手优化”。
