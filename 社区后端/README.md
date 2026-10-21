# community-backend —— 内容 / 知识社区后端

> 一个面向「Java 后端实习 / 校招」简历的**企业级日常开发后台**：用一套干净的分层代码，把 RBAC、事务、幂等、缓存防击穿、消息异步解耦、单元测试、可观测性完整跑通，并挂一个 2026 年加分项——**把 LLM 当「概率性下游组件」做工程化治理（AI 内容增强）**。
>
> 技术栈贴合 2026 年一线大厂 / 中厂 Java 实习 JD：**Spring Boot 3 + Java 21 + MyBatis-Plus + Redis + 异步解耦（RabbitMQ 可切换）+ JWT/RBAC + 虚拟线程**。

---

## 一、为什么写这个项目（简历定位）

招聘侧真正卡的是「**能干活**」的日常开发能力，不是堆名词。本项目刻意覆盖 JD 里反复出现的日常模块：

- 用户 / 权限（RBAC + JWT）
- 内容发布 / 评论 / 点赞（CRUD + 缓存 + 索引优化）
- 消息通知（MQ 异步解耦）
- 运营后台（分页、鉴权、审计）
- 一个 AI 亮点模块（内容摘要 / 标签 / 合规审核）

**简历一句话**：独立设计并实现了一个内容社区后端，覆盖鉴权、缓存防击穿、消息异步解耦与 AI 增强，并用 JUnit5 + Mockito 保证核心逻辑可测。

---

## 二、技术栈

| 维度 | 选型 | 说明 |
| --- | --- | --- |
| 语言 / 框架 | Java 21 + Spring Boot 3.4.5 | 虚拟线程、Record、Jakarta |
| 持久层 | MyBatis-Plus 3.5.7 + MySQL 8（本地 H2 兼容模式） | 逻辑删除、分页插件、条件构造器 |
| 缓存 | Redis（Cache-Aside + 分布式锁） | 热点帖子缓存、防击穿、防穿透 |
| 异步解耦 | 本地虚拟线程异步 / RabbitMQ（prod 可切） | 发布即发通知，主链路不阻塞 |
| 安全 | Spring Security + JWT（HS256）+ RBAC | 无状态、方法级 `@PreAuthorize` |
| AI 亮点 | 自研 DeepSeek 客户端（OpenAI 兼容协议） | 无 Key 时规则降级，离线可跑 |
| 工程化 | Maven + JUnit5 + Mockito + Actuator | 单测、可观测性、统一响应 |

---

## 三、快速开始（零外部依赖即可跑）

环境要求：**JDK 21**、**Redis 6379**（本机已在跑）、Maven 3.9+。MySQL / RabbitMQ 仅在 `prod` profile 需要，本地用 `local` profile 即可。

```bash
# 1. 打包
mvn -q -DskipTests package

# 2. 本地启动（H2 文件库 + 原生 Redis + 虚拟线程异步，无需 MySQL/MQ）
java -jar target/community-backend.jar --spring.profiles.active=local --server.port=8088
```

启动后：

- 接口前缀：`http://127.0.0.1:8088/api`
- H2 控制台：`http://127.0.0.1:8088/h2-console`（JDBC URL：`jdbc:h2:file:./data/community`）
- 健康检查：`GET /api/actuator/health`

**种子账号**（启动时自动初始化）：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| admin | admin123 | ADMIN |
| alice | alice123 | USER |
| bob | bob123 | USER |

### 接口示例（curl）

```bash
# 登录拿到 token
TOKEN=$(curl -s -X POST http://127.0.0.1:8088/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')

# 发帖（需登录）
curl -s -X POST http://127.0.0.1:8088/api/posts \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Spring Boot 缓存实战","content":"讲解 Cache-Aside + 分布式锁防击穿"}'

# 帖子详情（公开，走 Redis 缓存）
curl -s http://127.0.0.1:8088/api/posts/1

# AI 增强（无 Key 走规则降级，返回 summary/tags/safe）
curl -s -X POST http://127.0.0.1:8088/api/ai/posts/1/enhance -H "Authorization: Bearer $TOKEN"
```

---

## 四、整体架构

```
┌────────────┐   ┌─────────────────────────── 应用分层 ───────────────────────────┐
│  Controller │──▶│  Service(事务/幂等/缓存编排)  ──▶  Mapper(MyBatis-Plus)  ──▶ DB │
└────────────┘   │        │                          │                              │
                 │        ├──▶ PostCacheService ──▶ Redis（Cache-Aside + 分布式锁） │
                 │        ├──▶ ContentAiService ──▶ DeepSeek / 规则降级             │
                 │        └──▶ NotificationPublisher ──▶ 本地异步 / RabbitMQ         │
                 └─────────────────────────────────────────────────────────────────┘
                                  Security: JwtAuthFilter + @PreAuthorize(RBAC)
```

**模块清单**

| 模块 | 能力 | 关键设计 |
| --- | --- | --- |
| `user` | 注册 / 登录 / 当前用户 / 运营分页 | BCrypt 加密、JWT 签发校验、RBAC 方法级鉴权 |
| `content` | 帖子 CRUD / 详情 / 评论 / 点赞 | 缓存防击穿、分布式锁、点赞幂等 |
| `notify` | 通知列表 / 未读计数 / 已读 | 发布解耦，本地异步与 RabbitMQ 双实现可切换 |
| `ai` | 帖子摘要 / 标签 / 合规审核 | LLM 当下游组件，失败/无 Key 降级规则实现 |

---

## 五、技术亮点与面试深挖

### 1. JWT + RBAC 无状态鉴权
- **为什么用 RBAC 而不是 ACL**：RBAC 按「角色」授权，新增权限只需调整角色，适合社区这种角色固定的场景；ACL 按「用户-资源」细粒度授权，数据量爆炸、维护成本高。
- **token 续期**：access token 短期有效（默认 120 分钟），配合刷新 token 或「滑动过期」；本项目演示用单 token + 合理有效期，生产可加 refresh token 表。
- **痛点**：JWT 无法主动吊销——吊销需引入黑名单（Redis 存 jti）或缩短有效期。

### 2. 缓存防击穿（Cache-Aside + 分布式锁）
- 读路径：`Redis 命中直接返回` → `未命中则竞争分布式锁` → `仅一个线程回源 DB 并写缓存` → `其余线程自旋/短暂等待后读缓存`。
- 锁用 `SET NX` + Lua 原子释放（防误删他人锁），避免「缓存击穿」把 DB 打挂。
- **防穿透**：空结果也缓存短时长（或布隆过滤器），避免不存在的 ID 反复打 DB。
- **防雪崩**：缓存过期时间加随机抖动。
- **踩过的坑（已修，并有测试兜底）**：初版抢不到锁就直接抛 `IllegalStateException`，实测热点 key 失效瞬间
  **20 个并发请求里 19 个直接 500**——防击穿反而制造了故障，比直接打 DB 还糟。
  现改为 `RedisLock#lockWithWait`：抢到锁的线程回源写缓存，其余线程短暂自旋等缓存回填，
  自旋超时才兜底自己回源（可用性优先于「只回源一次」）。

### 3. 消息异步解耦
- 发帖 / 评论后通过 `NotificationPublisher` 推送通知，**主链路不等待**通知落库。
- `local` profile 用 `@Async` + 虚拟线程实现；`prod` profile 切到 RabbitMQ（保证消息不丢：publisher confirm + 消费者手动 ACK + 死信队列）。
- **重复消费**：通知写入带业务唯一键（receiver+type+refId），消费幂等。

### 4. 点赞幂等
- 点赞表对 `(post_id, user_id)` 建唯一索引，重复点赞走 `ON DUPLICATE` / 先查后插，天然幂等；取消点赞仅切换状态。
- **踩过的坑（已修，并有测试兜底）**：计数曾用「读-改-写」（`setLikeCount(getLikeCount()+1)` + `updateById`），
  实测 10 个用户并发点赞后 `like_count` **只剩 1**，丢了 9 次更新。
  现改为 `UPDATE ... SET like_count = CASE ... END` 原子自增，并发下计数与 `t_post_like` 真实行数严格一致。

### 5. AI 模块：把 LLM 当「概率性下游组件」治理（2026 加分项）
- 自研轻量 DeepSeek 客户端（基于 OpenAI 兼容协议 `RestClient`），**不引入重框架**。
- **核心治理思路**：LLM 会超时、会返回非 JSON、会胡说 → 必须降级。未配 Key 或调用失败 → 规则实现兜底（摘要=截取前 80 字、标签=默认分类、合规=关键词过滤）。
- 结果写回帖子（`ai_enhanced` 标记），页面无感知地获得 AI 能力。

### 6. JDK 21 虚拟线程
- 异步通知执行器用 `VirtualThreadPerTaskExecutor`，高并发下相比平台线程大幅降低线程开销（演示场景量级有限，但选型理由要讲清）。

### 7. 可观测性与规范
- 统一响应 `Result<T>` / `PageResult<T>`，全局异常收敛业务码（10002 未登录 / 10003 无权限）。
- Actuator 暴露 `health/info/metrics`，日志分级。
- `context-path=/api` 下 Security 的 `requestMatchers` 必须用**剥离前缀后的路径**（踩过的坑，见下文）。

---

## 五·五、实测 Benchmark（真实数据，非估算）

> 工具：`benchmark.py`（Python 标准库，零三方依赖，本地单进程 client+server 共机压测）。
> 目标接口：`GET /api/posts/1`（详情，公开读）。运行环境：JDK 21 + 内嵌 Tomcat + 本地 H2(内存) + 原生 Redis(6379)。

### 实测结果（2026-10-16，单机 localhost）

| 场景 | 并发 | 样本 | 平均 | p50 | p95 | p99 | 吞吐 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 缓存命中（warm 后串行） | 1 | 300 | 13.2 ms | 15.4 ms | 16.5 ms | 26.0 ms | — |
| 缓存未命中（每次删 key 回源） | 1 | 100 | 10.0 ms | 4.3 ms | 22.5 ms | 27.4 ms | — |
| 缓存命中（高并发） | 40 | 2000 | 7.5 ms | 7.6 ms | 10.2 ms | 14.8 ms | **~2574 QPS** |

### 诚实结论（重要，面试必考）

1. **本地 H2 是内存库，命中/未命中延迟差落在噪声里**（都在 5–15ms 区间，p99 基本持平 ~26ms）。
   这不是缓存"没用"，而是**DB 回源本身已是亚毫秒级**——缓存的延迟收益在本地被抹平了。
2. **缓存的真实价值在"规模化"与"DB 保护"**：换上网络 MySQL(LAN) 后，未命中需承担 5–20ms+ 的 DB 往返，
   命中路径仍只走 Redis(亚毫秒)，此时差距才会拉开；且高并发下缓存能把 DB QPS 降一个数量级，避免 DB 成为瓶颈。
3. 吞吐 ~2574 QPS 是**单机、共机、无调优**下限，仅说明该接口无性能陷阱；生产应结合连接池、索引、CDN 再量化。

> ⚠️ 不编造假数字：上面的 p99 / QPS 是真实跑出来的；"MySQL 下缓存提速 X 倍"属合理推断，面试时讲**思路+原因**即可，
> 若被追问精确值，就如实说"本地是 H2 测不出差距，生产需以压测为准"——这比背数字更显老练。

---

## 六、高频面试题 & 回答要点

| 面试官可能问 | 你能讲的点 |
| --- | --- |
| 缓存和数据库一致性怎么保证？ | Cache-Aside：写时删缓存（或更新后删），读时回源补。容忍短暂不一致，用过期时间兜底；对一致性要求高的字段走「先更新 DB 再删缓存 + 延时双删」。 |
| 缓存击穿 / 穿透 / 雪崩区别？ | 击穿=热点 key 失效瞬间大量请求打 DB（用分布式锁）；穿透=查不存在的数据（空值缓存/布隆过滤器）；雪崩=大量 key 同时失效（过期加随机）。 |
| 分布式锁怎么实现？坑？ | `SET NX` + 过期时间；释放用 Lua 保证原子；坑：锁未设超时会死锁、超时过短业务没跑完锁就释放（看门狗续期）、误删他人锁（value 存唯一标识）。 |
| 为什么用消息队列？ | 解耦、异步、削峰。本项目发帖后通知解耦，主链路响应快；MQ 还能做重试与削峰。 |
| 怎么保证消息不丢 / 不重复？ | 不丢：publisher confirm + 持久化 + 手动 ACK + 死信；不重复：消费端幂等（唯一键 / 去重表）。 |
| 你负责什么 / 难点？ | 独立设计表结构与缓存防击穿方案，重点解决了 Redis 与 DB 一致性和高并发下锁竞争；AI 模块的降级治理是关键亮点。 |
| 怎么证明缓存防击穿 / 幂等真的生效？ | 不靠嘴说：写了需要真实 Redis 的集成测试。并发 20 个请求打同一个冷 key，断言「DB 浏览量只 +1」等价于「只回源 1 次」；并发 10 个用户点赞，断言 `like_count` 等于真实行数。这两个测试在修复前都是红的（19/20 抛异常、计数只剩 1），修复后才转绿。 |
| 有没有压测数据？ | 有（见「五·五、实测 Benchmark」）：单机 localhost 下详情接口 p99 ~26ms、高并发吞吐 ~2574 QPS；本地 H2 内存库使命中/未命中延迟差在噪声内，缓存真实收益在换网络 MySQL 后体现——这恰好是架构取舍的考点。 |

> ⚠️ 诚实原则：上表 p99 / QPS 是 `benchmark.py` 真实跑出来的；但"MySQL 下缓存提速 X 倍"属推断，面试讲**原因+思路**即可，被追问精确值就如实说"本地 H2 测不出，以生产压测为准"。别背假数字。

---

## 七、测试

```bash
mvn test
```

共 **16 个用例 / 5 个测试类**，分两类。

### 1) 纯单测（6 例，离线可跑，不依赖外部组件）

| 测试类 | 覆盖点 |
| --- | --- |
| `JwtTokenProviderTest` | 签发 / 解析 / 过期 / 篡改失效 |
| `ContentAiServiceTest` | 无 Key 时规则降级、敏感词判定、长文摘要截断 |

### 2) 集成测试（10 例，需要真实 Redis；`test` profile 用 H2 内存库）

Redis 不可用时由 `RedisAvailability` 门卫自动 **skip**，不会让整条流水线变红。

| 测试类 | 覆盖点 | 核心断言 |
| --- | --- | --- |
| `RedisLockIntegrationTest` | 互斥、防误删他人锁、自动过期防死锁、自旋等回源 | 20 线程并发只有 1 个真正回源，且无人抛异常 |
| `PostCacheBreakdownIntegrationTest` | 命中不回源、并发防击穿、空值防穿透 | 浏览量只在回源时 +1 ⇒ 20 并发未命中后 DB 浏览量仍为 1 |
| `LikeIdempotencyIntegrationTest` | 点赞 toggle、并发计数一致、唯一索引兜底 | `t_post.like_count` 恒等于 `t_post_like` 真实行数 |

### 集成测试不是摆设：修复前后实测对比

把实现改回旧写法再跑，测试立刻变红——这是它们确实有牙齿的证据：

| 场景 | 旧实现实测 | 修复后 |
| --- | --- | --- |
| 20 并发打同一个冷 key | **19/20 请求抛 `IllegalStateException`** | 0 抛错，只回源 1 次 |
| 10 个用户并发点赞 | **`like_count` 只剩 1**（丢 9 次更新） | `like_count == 10`，与行数一致 |

---

## 八、生产部署（prod profile）

`application-prod.yml` 已预留 MySQL + RabbitMQ 配置：

```bash
# 前置：MySQL 8、RabbitMQ 3.x、Redis
java -jar community-backend.jar --spring.profiles.active=prod
```

要点：

- 表结构用 `src/main/resources/db/schema-mysql.sql`（MySQL 8 语法，与 H2 版一一对应；
  索引与唯一键内联声明，表已存在时整句跳过，脚本可重复执行）。
- `community.mq.type=rabbitmq` 启用 MQ 发布消费；`community.ai.api-key` 注入 DeepSeek Key 启用真实模型。
- JWT 密钥务必通过环境变量注入，不要提交到仓库。

---

## 九、目录结构

```
community-backend
├── pom.xml
├── src/main/java/com/luoxianjun/community
│   ├── common/        # 统一响应、异常、Redis 锁、限流注解/切面
│   ├── config/        # MyBatis-Plus 分页、异步(虚拟线程)、Security、数据源
│   ├── security/      # JWT 工具、过滤器、RBAC
│   └── module/
│       ├── user/      # 用户与鉴权
│       ├── content/   # 帖子/评论/点赞 + 缓存防击穿
│       ├── notify/    # 通知异步解耦（本地/RabbitMQ）
│       └── ai/        # DeepSeek 客户端 + 内容增强(降级)
├── src/main/resources
│   ├── application.yml / -local.yml / -prod.yml
│   └── db/schema-h2.sql
└── src/test/          # JUnit5 + Mockito 单测
```

---

*项目定位：实习 / 校招简历的可讲、可深挖日常开发样例。聚焦「能干活」，不堆微服务名词。*
