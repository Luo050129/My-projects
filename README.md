# My Projects

个人项目集合。一个项目一个顶层目录，各自独立构建、独立说明。

## 项目

| 项目 | 简介 | 技术栈 |
| --- | --- | --- |
| [君山点评](./君山点评/) | 本地生活 / 美食点评平台，实现短信登录、商户缓存、优惠券秒杀、附近商户、UV 统计、签到、关注、探店等业务闭环，重点用 Redis 处理高并发与分布式场景 | Spring Boot 2.3 · MyBatis-Plus · Redis（GEO / BitMap / HyperLogLog / ZSet / Redisson）· RabbitMQ · MySQL 8 · Vue 3 |
| [社区后端](./社区后端/) | 内容 / 知识社区后端，覆盖 RBAC 鉴权、缓存防击穿、消息异步解耦与 AI 内容增强；16 个自动化测试（含 10 个依赖真实 Redis 的并发集成测试） | Java 21 · Spring Boot 3.4.5 · MyBatis-Plus · Redis · Spring Security + JWT · RabbitMQ · H2 / MySQL |

## 目录约定

```
My-projects/
├── 君山点评/                 # 本地生活点评平台（Spring Boot + Vue 3）
│   ├── src/                  # 后端源码
│   ├── frontend/             # 前端源码
│   ├── pom.xml               # 后端构建
│   └── README.md             # 该项目的说明与运行方式
├── 社区后端/                 # 内容 / 知识社区后端（Java 21 + Spring Boot 3）
│   ├── src/                  # 后端源码
│   ├── pom.xml
│   ├── benchmark.py          # 压测脚本
│   └── README.md
├── .github/                  # 仓库级配置
└── .gitignore                # 仓库级忽略规则
```

- 新增项目：在根目录建同名文件夹，自带 `README.md` 与构建脚本；
- 根目录只放仓库级文件，不放任何项目的源码；
- 敏感配置（数据库 / Redis 密码）不入库，按各项目内的 `application.example.yaml` 复制后本地填写。

## 环境

各项目依赖不同，详见对应目录的 README。

- **君山点评**：JDK 8+、MySQL 8、Redis 5+；秒杀异步链路另需 RabbitMQ
- **社区后端**：JDK 21、Redis（本地 `local` profile 用 H2，无需 MySQL / RabbitMQ）
