# My Projects

个人项目集合。一个项目一个顶层目录，各自独立构建、独立说明。

## 项目

| 项目 | 简介 | 技术栈 |
| --- | --- | --- |
| [君山点评](./君山点评/) | 本地生活 / 美食点评平台，实现短信登录、商户缓存、优惠券秒杀、附近商户、UV 统计、签到、关注、探店等业务闭环，重点用 Redis 处理高并发与分布式场景 | Spring Boot 2.3 · MyBatis-Plus · Redis（GEO / BitMap / HyperLogLog / ZSet / Redisson）· RabbitMQ · MySQL 8 · Vue 3 |

## 目录约定

```
My-projects/
├── 君山点评/                 # 一个项目一个顶层目录，目录名即项目名
│   ├── src/                  # 后端源码
│   ├── frontend/             # 前端源码
│   ├── pom.xml               # 后端构建
│   └── README.md             # 该项目的说明与运行方式
├── .github/                  # 仓库级配置
└── .gitignore                # 仓库级忽略规则
```

- 新增项目：在根目录建同名文件夹，自带 `README.md` 与构建脚本；
- 根目录只放仓库级文件，不放任何项目的源码；
- 敏感配置（数据库 / Redis 密码）不入库，按各项目内的 `application.example.yaml` 复制后本地填写。

## 环境

各项目依赖不同，详见对应目录的 README。君山点评需要 JDK 8+、MySQL 8、Redis 5+，秒杀异步链路另需 RabbitMQ。
