<p align="center">
  <img alt="OpenIsle" src="https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/image.png" width="200">
  <br><br>
  高效的开源社区后端平台
  <br><br>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square"></a>
</p>

## 💡 简介

OpenIsle 基于 Spring Boot 构建，提供社区后台常见的注册、登录、文章与评论等能力，适合用作二次开发的起点，完全开源。

## ⚡ 动机

* 下一代的可扩展的社区或论坛应用
* 提供清晰的代码示例和简单易懂的配置

## ✨ 特性

* **用户体系**：注册、登录，密码使用 BCrypt 加密
* **JWT 认证**：登录后获得 Token，接口通过 `Authorization: Bearer` 认证
* **Google 登录**：支持使用 Google OAuth 登录
* **邮件通知**：抽象 `EmailSender`，默认实现基于 Resend
* **角色权限**：内置 `ADMIN` 与 `USER`，管理员接口以 `/api/admin/**` 提供
* **文章与评论**：支持分类、评论及多级回复
* **标签管理**：普通用户只能查看标签，创建或删除仅限管理员
* **图片上传**：`ImageUploader` 可接入不同云存储，示例中实现了腾讯云 COS
* **灵活配置**：数据库、邮件、存储及密码强度均可通过环境变量或 `application.properties` 设置
* **简洁架构**：业务、持久化与安全配置清晰分层，便于扩展

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3+
- MySQL 数据库
- Docker（可选，便于本地开发）

### 运行

1. 按需修改 `src/main/resources/application.properties` 或设置以下环境变量：
   - `MYSQL_URL`：数据库连接 URL，如 `jdbc:mysql://localhost:3306/openisle`
   - `MYSQL_USER`：数据库用户名
   - `MYSQL_PASSWORD`：数据库密码
   - `RESEND_API_KEY`：Resend 邮件服务 API Key
   - `COS_BASE_URL`：腾讯云 COS 访问域名
   - `GOOGLE_CLIENT_ID`：Google OAuth 客户端 ID
   - `VUE_APP_GOOGLE_CLIENT_ID`：前端 Google OAuth 客户端 ID
   - `JWT_SECRET`：JWT 签名密钥
   - `JWT_EXPIRATION`：JWT 过期时间（毫秒）
   - `PASSWORD_STRENGTH`：密码强度（LOW、MEDIUM、HIGH）
   - `CAPTCHA_ENABLED`：是否启用验证码（true/false）
   - `RECAPTCHA_SECRET_KEY`：Google reCAPTCHA 密钥
   - `CAPTCHA_REGISTER_ENABLED`：注册是否需要验证码
   - `CAPTCHA_LOGIN_ENABLED`：登录是否需要验证码
   - `CAPTCHA_POST_ENABLED`：发帖是否需要验证码
   - `CAPTCHA_COMMENT_ENABLED`：评论是否需要验证码
   - `OPENAI_API_KEY`：OpenAI 接口密钥
   - `OPENAI_MODEL`：调用的模型名称，默认为 `gpt-4o`

在本地没有 MySQL 环境时，可先执行
```bash
docker-compose up -d
```
来启动随附的数据库容器。
2. 启动项目：

```bash
mvn spring-boot:run
```

启动后可访问主要接口：

- `POST /api/auth/register`：注册新用户
- `POST /api/auth/login`：登录并获取 Token
- `POST /api/auth/google`：Google 登录并获取 Token
- `GET /api/config`：查看验证码开关配置
- 需要认证的接口示例：`GET /api/hello`（需 `Authorization` 头）
- 管理员接口示例：`GET /api/admin/hello`

## 🏘️ 社区

欢迎通过 [Issues](https://github.com/nagisa77/OpenIsle/issues) 交流反馈。

## 📄 授权

本项目以 MIT License 发布，欢迎自由使用与修改。

## 🙏 鸣谢

- [Spring Boot](https://spring.io/projects/spring-boot)
- [JJWT](https://github.com/jwtk/jjwt)
- [Lombok](https://github.com/projectlombok/lombok)
- 以及所有开源贡献者

