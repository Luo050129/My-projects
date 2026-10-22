# My Projects

个人项目集合。一个项目一个顶层目录，各自独立构建、独立说明。

## 项目

| 项目 | 简介 | 技术栈 |
| --- | --- | --- |
| [君山点评](./君山点评/) | 本地生活 / 美食点评平台，实现短信登录、商户缓存、优惠券秒杀、附近商户、UV 统计、签到、关注、探店等业务闭环，重点用 Redis 处理高并发与分布式场景 | Spring Boot 2.3 · MyBatis-Plus · Redis（GEO / BitMap / HyperLogLog / ZSet / Redisson）· RabbitMQ · MySQL 8 · Vue 3 |
| [知渡](./知渡/) | 内容 / 知识社区后端，围绕「读多写少、热点集中」做缓存治理，覆盖 RBAC 鉴权、缓存防击穿、通知异步解耦与 AI 内容增强；16 个自动化测试（含 10 个依赖真实 Redis 的并发集成测试） | Java 21 · Spring Boot 3.4.5 · MyBatis-Plus · Redis · Spring Security + JWT · RabbitMQ · H2 / MySQL |
| [打字竞速-EasyX](./打字竞速-EasyX/) | 双人在线打字竞速游戏：打字推进进度，角色沿折线赛道赛跑，通过 HTTP 与服务端互同步进度；含向量 / 摄像机 / 帧动画 / 角色 / 赛道等引擎骨架（**仅头文件，缺主程序与资源**） | C++ · EasyX（AlphaBlend 透明贴图）· cpp-httplib · Windows MCI 音频 |

## 目录约定

```
My-projects/
├── 君山点评/                 # 本地生活点评平台（Spring Boot + Vue 3）
│   ├── src/                  # 后端源码
│   ├── frontend/             # 前端源码
│   ├── pom.xml               # 后端构建
│   └── README.md             # 该项目的说明与运行方式
├── 知渡/                     # 内容 / 知识社区后端（Java 21 + Spring Boot 3）
│   ├── src/                  # 后端源码
│   ├── pom.xml
│   ├── benchmark.py          # 压测脚本
│   └── README.md
├── 打字竞速-EasyX/            # 双人打字竞速游戏（C++ / EasyX）
│   ├── include/              # 9 个头文件（引擎骨架）
│   └── README.md             # 玩法、模块职责、缺失清单与已知问题
├── .github/                  # 仓库级配置
└── .gitignore                # 仓库级忽略规则
```

- 新增项目：在根目录建同名文件夹，自带 `README.md` 与构建脚本；
- 根目录只放仓库级文件，不放任何项目的源码；
- 敏感配置（数据库 / Redis 密码）不入库，按各项目内的 `application.example.yaml` 复制后本地填写。

## 环境

各项目依赖不同，详见对应目录的 README。

- **君山点评**：JDK 8+、MySQL 8、Redis 5+；秒杀异步链路另需 RabbitMQ
- **知渡**：JDK 21、Redis（本地 `local` profile 用 H2，无需 MySQL / RabbitMQ）
- **打字竞速-EasyX**：Visual Studio + EasyX 图形库（Windows）；**当前仅含头文件骨架，缺主程序与资源，无法直接编译**，补齐清单见其 README
