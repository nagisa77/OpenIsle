<p align="center">
  <img alt="OpenIsle" src="https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/image.png" width="200">
  <br>
  高效的开源社区前后端平台
  <br><br><br>
  <img alt="Image" src="https://openisle-1307107697.cos.accelerate.myqcloud.com/dynamic_assert/22752cfac5a04a9c90c41995b9f55fed.png" width="1200">
    <br><br><br>
  <a href="https://hellogithub.com/repository/nagisa77/OpenIsle" target="_blank"><img src="https://abroad.hellogithub.com/v1/widgets/recommend.svg?rid=8605546658d94cbab45182af2a02e4c8&claim_uid=p5GNFTtZl6HBAYQ" alt="Featured｜HelloGitHub" style="width: 250px; height: 54px;" width="250" height="54" /></a>
</p>

## 💡 简介

OpenIsle 是一个使用 Spring Boot 和 Vue 3 构建的全栈开源社区平台，提供用户注册、登录、贴文发布、评论交互等完整功能，可用于项目社区或直接打造自主社区站点。

## 🚧 开发 & 部署

详细见 [Contributing](https://github.com/nagisa77/OpenIsle?tab=contributing-ov-file)

## ✨ 项目特点

- JWT 认证以及 Google、GitHub、Discord、Twitter 等多种 OAuth 登录
- 支持分类、标签的贴文管理以及草稿保存功能
- 嵌套评论、指定贴文或评论的点赞/抖弹系统
- 定制统计和通知消息，包括日活跃用户等数据
- 全局搜索，支持用户和很多内容的搜索以及内容缩略
- 集成 OpenAI 提供的 Markdown 格式化功能
- 通过环境变量可调整密码强度、登录方式、保护码等多种配置
- 支持图片上传，默认使用腾讯云 COS 扩展
- 默认头像使用 DiceBear Avatars，可通过 `AVATAR_STYLE` 和 `AVATAR_SIZE` 环境变量自定义主题和大小
- 浏览器推送通知，离开网站也能及时收到提醒
- 新增 Python MCP 搜索服务，方便 AI 助手通过统一协议检索社区内容

## 🌟 项目优势

- 全面开源，便于二次开发和自定义扩展
- Spring Boot + Vue 3 成熟技术栈，学习起点低，社区资源丰富
- 支持多种登录方式和角色权限，容易展展到不同场景
- 模块化设计，代码结构清晰，维护成本低
- REST API 可接入任意前端框架，兼容多端平台
- 配置简单，通过环境变量快速调整和部署
- 如需推送通知，请设置 `WEBPUSH_PUBLIC_KEY` 和 `WEBPUSH_PRIVATE_KEY` 环境变量

## 🏘️ 社区

欢迎彼此交流和使用 OpenIsle，项目以开源方式提供，想了解更多可访问：<https://github.com/nagisa77/OpenIsle>

## 📋 授权

本项目以 MIT License 发布，欢迎自由使用与修改。

## 🙏 鼓赞

- [Spring Boot](https://spring.io/projects/spring-boot)
- [JJWT](https://github.com/jwtk/jjwt)
- [Lombok](https://github.com/projectlombok/lombok)
- 以及所有开源贡献者
