# Deploy 协作指引

## 1) 适用范围

- 作用于 `deploy/` 目录及其脚本。
- 该目录为高风险变更区，默认保守修改。

## 2) 当前部署基线

- 预发：`main` push 触发（见 `.github/workflows/deploy-staging.yml`）。
- 正式：定时任务触发（见 `.github/workflows/deploy.yml`）。
- 两者使用同一并发锁 `openisle-server`，避免服务器并发部署冲突。

## 3) 脚本修改原则

- 保留 `set -euo pipefail` 等安全执行特性。
- 变更服务列表或 `docker compose up` 参数时，必须说明影响范围。
- 不随意改动 `git fetch/checkout/reset` 逻辑；若改，必须附回滚方案。
- 任何“可能中断服务”的改动，都要在说明中给出停机/风险评估。

## 4) 环境与参数规则

- 部署依赖根目录 `.env`（由脚本中 `env_file` 与 `ENV_FILE` 传入）。
- `COMPOSE_PROJECT_NAME`、`NUXT_ENV`、服务名列表需保持可追踪且与 compose 一致。
- 若新增服务，需同步：
  - `docker/docker-compose.yaml`
  - 部署脚本中的 build/up 目标
  - 必要时更新 workflow 说明

## 5) 验证建议

- 语法检查：
  - `bash -n deploy/deploy.sh`
  - `bash -n deploy/deploy_staging.sh`
- 变更前后做一次 `docker compose config` 思维核对（服务与 profile 是否正确）。

## 6) 输出要求

- 明确：影响环境（预发/正式）、影响服务、是否可能重建容器。
- 必填：回滚路径（例如切回上一 commit 并重新执行部署脚本）。
