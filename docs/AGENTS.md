# Docs 协作指引

## 1) 适用范围

- 作用于 `docs/` 目录及其子目录。
- 文档需服务“开发者真实使用”，优先准确性与可执行性。

## 2) 文档架构

- 内容目录：`content/docs/`
- 生成脚本：`scripts/generate-docs.ts`
- OpenAPI 输入配置：`lib/openapi.ts`
- 前端框架：Fumadocs + Next.js（Bun 工具链）

## 3) 编辑规则

- 优先修正“与代码不一致”的文档，不复制过时描述。
- 涉及技术栈说明时，以当前代码为准（例如后端为 JPA/Repository）。
- OpenAPI 自动生成目录（`content/docs/openapi/(generated)`）不要手工细改，改源头配置与脚本。
- 结构性改动优先维持导航稳定（`meta.json` 与已有 slug）。

## 4) OpenAPI 同步规则

- 后端 API 变更后，应重新生成文档页面：
  - `bun run generate`
- 若接口来源地址或文档聚合策略变化，更新：
  - `lib/openapi.ts`
  - `scripts/generate-docs.ts`

## 5) 验证命令

- 安装依赖：`bun install`
- 生成 API 文档：`bun run generate`
- 构建校验：`bun run build`
- 本地预览：`bun dev`

## 6) 输出要求

- 说明更新了哪些文档入口（backend/frontend/openapi）。
- 说明是否需要后端先部署后再刷新文档产物。
