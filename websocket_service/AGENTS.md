# WebSocket Service 协作指引

## 1) 适用范围

- 作用于 `websocket_service/` 目录及其子目录。
- 本服务是实时通知链路关键节点，改动需谨慎。

## 2) 服务职责

- 通过 STOMP 维护客户端实时连接（`/api/ws`、`/api/sockjs`）。
- 从 RabbitMQ 队列消费通知并转发至用户/会话目的地。
- 在连接阶段执行 JWT 鉴权。

## 3) 关键一致性规则

- JWT 密钥与后端保持一致（同一 `JWT_SECRET` 语义）。
- 队列配置与后端分片策略同步：
  - 后端声明：`backend/.../RabbitMQConfig.java`
  - 后端分片：`backend/.../ShardingStrategy.java`
  - 本服务监听：`src/main/java/com/openisle/websocket/listener/NotificationListener.java`
- 监听队列当前约定：16 个十六进制分片队列 + 遗留 `notifications-queue`。

## 4) 修改规则

- 不随意变更 STOMP 目的地命名（`/topic/...`、`/user/...`）。
- 若必须调整目的地，需同步前端 `frontend_nuxt/composables/useWebSocket.js` 与相关消费代码。
- `WebSocketAuthInterceptor` 中 CONNECT 鉴权失败策略（拒绝连接）应保持清晰一致。
- Allowed origins 改动需考虑本地、预发、正式环境域名。

## 5) 配置与可观测性

- 配置入口：`src/main/resources/application.properties`
- 健康检查：`/actuator/health`（部署与 compose 依赖该路径）
- 日志级别改动需避免在生产产生高噪声。

## 6) 验证建议

- 首选：`mvn test`
- 若暂无测试覆盖：`mvn -DskipTests compile`
- 变更消息推送逻辑时，至少完成一次端到端验证：
  - 生产者发送消息
  - RabbitMQ 消费成功
  - 客户端收到对应目的地消息

## 7) 输出要求

- 说明是否影响队列名、路由键、目的地或鉴权逻辑。
- 说明是否需要前端/后端同步改动。
