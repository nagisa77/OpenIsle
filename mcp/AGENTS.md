# MCP 服务协作指引

## 1) 适用范围

- 作用于 `mcp/` 目录及其子目录。
- 本模块对外提供 MCP tools，接口兼容性要求高。

## 2) 模块结构

- `src/openisle_mcp/server.py`：Tool 定义与请求处理入口。
- `src/openisle_mcp/search_client.py`：调用 OpenIsle 后端 HTTP API。
- `src/openisle_mcp/schemas.py`：Pydantic 数据契约。
- `src/openisle_mcp/config.py`：运行配置与环境变量读取。

## 3) 变更原则

- Tool 名称默认视为稳定契约，非必要不重命名。
- 后端接口字段变化时，优先同步 `schemas.py`，再调整 `server.py` 映射。
- 对认证接口保持“显式失败”：
  - 缺 token、401、403 需给出可理解错误信息。
- 避免吞掉异常上下文，保留足够定位信息（HTTP 状态、接口语义）。

## 4) 与后端契约同步

- 高风险同步点：
  - `create_post`
  - `reply_to_post`
  - `reply_to_comment`
  - `list_unread_messages`
  - `mark_notifications_read`
- 若后端响应结构改动，需同步：
  - `search_client.py` 的解析逻辑
  - `schemas.py` 的校验模型
  - `README.md` 的 tool 说明（如有新增/删减）

## 5) 配置规则

- 环境变量统一使用 `OPENISLE_MCP_*` 前缀。
- 保持默认值可本地运行（如 `http://localhost:8080` 场景）。
- 不在代码中硬编码私密 token。

## 6) 验证建议

- 安装校验：`python -m pip install -e .`
- 启动校验：`openisle-mcp`（或项目内等价启动方式）
- 如改动 schema/解析逻辑，至少完成一次真实后端联调请求。

## 7) 输出要求

- 说明变更是否影响 tool 输入/输出契约。
- 说明是否要求调用方更新（参数名、字段、错误语义）。
