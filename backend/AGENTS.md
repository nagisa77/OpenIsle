# Backend 协作指引

## 1) 适用范围

- 作用于 `backend/` 目录及其子目录。
- 若与根 `AGENTS.md` 冲突，以本文件为准（仅后端范围）。

## 2) 代码结构心智模型

- `controller/`：接口层（入参校验、权限边界、响应格式）。
- `service/`：业务编排与领域规则（核心逻辑放这里）。
- `repository/`：JPA 数据访问（基于实体与查询方法）。
- `model/`：实体模型与枚举。
- `dto/` + `mapper/`：对外契约和映射转换。
- `config/`：安全、缓存、MQ、OpenAPI、初始化器等基础设施配置。
- `search/`：OpenSearch 索引与事件驱动同步。

## 3) 后端修改规则

- 控制器保持“薄”，复杂逻辑下沉到 `service/`。
- DTO 变更优先考虑兼容性，避免无版本的破坏性字段删除/改名。
- 新增接口时：
  - 补齐必要的鉴权规则（`SecurityConfig`）。
  - 补齐 OpenAPI 注解（`@Operation`、`@ApiResponse` 等）。
- 涉及缓存时，确认 `CachingConfig` 中缓存名、TTL 与失效策略一致。

## 4) 重点一致性检查

- 鉴权与公开接口：
  - `src/main/java/com/openisle/config/SecurityConfig.java`
- 搜索索引同步（实体字段/文案变更时）：
  - `src/main/java/com/openisle/search/SearchDocumentFactory.java`
  - `src/main/java/com/openisle/search/SearchIndexEventPublisher.java`
- 消息通知链路（评论/通知相关）：
  - `src/main/java/com/openisle/config/RabbitMQConfig.java`
  - `src/main/java/com/openisle/config/ShardingStrategy.java`
  - `src/main/java/com/openisle/service/NotificationProducer.java`
- 环境变量消费面：
  - `src/main/resources/application.properties`
  - 根目录 `.env.example`

## 5) 数据与事务注意事项

- 涉及多表写入时，明确事务边界，避免半成功状态。
- 避免在 Controller 直接操作 Repository。
- JPA 懒加载对象对外返回前应通过 DTO 映射，避免序列化副作用。

## 6) 测试与验证

- 首选全量：`mvn test`
- 变更集中时可先跑目标测试（示例）：
  - `mvn -Dtest=PostControllerTest test`
  - `mvn -Dtest=SearchServiceTest test`
- 涉及搜索/MQ 配置时，至少完成一次启动级验证或关键集成测试。

## 7) 输出要求

- 明确列出“接口/字段/权限/事件”是否发生变化。
- 若影响 `mcp/` 或 `docs/`，在结果中显式提示需同步改动。
