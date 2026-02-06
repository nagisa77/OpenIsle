# Bots 协作指引

## 1) 适用范围

- 作用于 `bots/` 目录及其子目录。
- 本文件用于统一 Bot 脚本开发、调度与发布规范。

## 2) 模块结构与职责

- `bot_father.ts`：Bot 基类，统一 Agent 初始化、MCP 工具接入、CLI 运行入口。
- `instance/reply_bot.ts`：常规互动回复 Bot（提及/评论自动处理）。
- `instance/open_source_reply_bot.ts`：开源问答 Bot（偏代码与贡献流程）。
- `instance/daily_news_bot.ts`：每日新闻帖 Bot。
- `instance/coffee_bot.ts`：早安抽奖帖 Bot。

## 3) 开发约定（新增/改造 Bot）

- 新 Bot 统一继承 `BotFather`，最少实现：
  - `getAdditionalInstructions()`
  - `getCliQuery()`
- 保持导出约定：`export const runWorkflow = ...`，并保留 `if (require.main === module)` CLI 入口。
- 不随意改动 `bot_father.ts` 的 MCP 工具白名单；若必须调整，需同步评估 `mcp/` 契约与线上可用性。

## 4) 环境变量与密钥规范

- 必需：`OPENAI_API_KEY`（缺失会直接失败）。
- 常用：
  - `OPENISLE_TOKEN`（用于 OpenIsle MCP 鉴权；GitHub Actions 中可映射不同 secret）
  - `APIFY_API_TOKEN`（天气 MCP 鉴权）
- News Bot 可选参数：
  - `DAILY_NEWS_CATEGORY_ID`
  - `DAILY_NEWS_TAG_IDS`
- 严禁在代码中硬编码真实 token；仅通过 CI secrets 或本地环境变量注入。

## 5) 工作流同步规则（与 GitHub Actions 对齐）

- 相关工作流：
  - `.github/workflows/reply-bots.yml`
  - `.github/workflows/open_source_reply_bot.yml`
  - `.github/workflows/news-bot.yml`
  - `.github/workflows/coffee-bot.yml`
- 若改脚本入口、依赖或 env 键名，必须同步更新对应 workflow。
- 若改触发节奏（cron）或 Bot 行为边界，需在变更说明中写明影响（频率、成本、风险）。

## 6) 行为约束（防重复/防失控）

- 回复类 Bot 需保持幂等：避免对同一上下文重复回复。
- 处理未读后应调用 `mark_notifications_read` 清理通知。
- 批量处理建议保持上限（当前提示词约定为最多 10 条）。
- 发帖类 Bot（news/coffee）必须控制 `create_post` 调用次数（一次任务最多一次发帖）。
- Open Source Reply Bot 保持专业技术风格，避免跑题到非开源问答。

## 7) 本地验证建议

- 依赖安装（与 CI 一致）：
  - `npm install --no-save @openai/agents tsx typescript`
- 单脚本运行示例：
  - `npx tsx bots/instance/reply_bot.ts`
  - `npx tsx bots/instance/open_source_reply_bot.ts`
  - `npx tsx bots/instance/daily_news_bot.ts`
  - `npx tsx bots/instance/coffee_bot.ts`
- 验证时至少确认：可启动、可调用 MCP、异常时退出码非 0。

## 8) 输出要求

- 说明改动影响哪个 Bot、哪个 workflow。
- 说明是否改变了工具调用边界（MCP tools / 发帖次数 / 回复策略）。
- 说明是否需要同步更新文档或运维配置。
