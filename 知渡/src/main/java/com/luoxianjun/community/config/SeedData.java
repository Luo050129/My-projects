package com.luoxianjun.community.config;

import java.util.List;

/**
 * 种子数据定义。
 * <p>
 * 与 {@link DataInitializer} 分开：这里只放"内容长什么样"，落库逻辑在初始化器里。
 * 帖子用数组下标被评论 / 点赞引用（{@code postIndex}），改顺序时记得一起改引用。
 */
final class SeedData {

    private SeedData() {
    }

    record SeedUser(String username, String password, String nickname, String role, int daysAgo) {
    }

    record SeedPost(String author, String title, String content, String summary, String tags,
                    String cover, String postType, int visibility, int hoursAgo) {
    }

    record SeedComment(String author, int postIndex, String content, int hoursAgo) {
    }

    /** 点赞：postIndex → 点赞用户名列表。同一用户对同一帖只能赞一次（唯一索引）。 */
    record SeedLikes(int postIndex, String... users) {
    }

    record SeedFavorite(String user, int postIndex, int hoursAgo) {
    }

    /** 关注：follower 关注 followee */
    record SeedFollow(String follower, String followee) {
    }

    static final List<SeedUser> USERS = List.of(
            new SeedUser("admin", "admin123", "管理员", "ADMIN", 120),
            new SeedUser("alice", "alice123", "Alice", "USER", 96),
            new SeedUser("bob", "bob123", "Bob", "USER", 88),
            new SeedUser("carol", "carol123", "Carol", "USER", 64),
            new SeedUser("david", "david123", "David", "USER", 40),
            new SeedUser("eve", "eve123", "Eve", "USER", 18)
    );

    static final List<SeedPost> POSTS = List.of(
            // 0
            new SeedPost("alice",
                    "Redis 缓存击穿：我踩过的三个坑",
                    """
                    上周把帖子详情接上 Redis 之后，压测时发现一个诡异现象：热点帖一失效，接口 P99 直接从 30ms 冲到 2s，数据库连接池瞬间打满。

                    第一个坑是只在读的时候加锁。缓存在热点 key 失效的瞬间，几十个请求同时回源，锁加了等于没加——因为抢到锁的线程查完就释放，其余线程醒来发现缓存还是空。

                    第二个坑是空值没处理。有人拿不存在的 id 疯狂请求，每次都绕过缓存打到库里。后来给空结果也写了一个短 TTL 的标记值，才把穿透堵住。

                    第三个坑最隐蔽：我在锁里查完库，序列化失败抛了异常，锁释放了但缓存没写进去。下一个请求进来又重复一遍。现在的写法是写缓存失败只记日志，不让它影响接口返回。

                    三个坑加起来其实就一句话：缓存回源不是"查一次库"，是要把并发、空值、异常这三件独立的事都处理掉。""",
                    "热点 key 失效瞬间并发回源、空结果穿透、写缓存异常，三个独立问题要分别处理",
                    "Redis,缓存,并发", "/api/seed/cover-redis.svg", "IMAGE", 0, 3),

            // 1
            new SeedPost("bob",
                    "从零搭一个 Spring Boot 项目，我建议先定这五件事",
                    """
                    很多人拿到需求就开始建 Controller，写到第三天开始返工。我现在起项目会先花半小时把这五件事定下来。

                    一、包结构按业务分还是按层分。小项目按层无所谓，一旦超过三个模块，按业务分（user/content/notify）更好找代码。

                    二、统一响应结构。Result 里到底是 code+message+data 还是直接抛异常，前端要不要区分业务错误和网络错误，这个不定，后面每个接口都会长不一样。

                    三、分页参数默认值。current 从 1 开始还是 0 开始，size 上限多少。我被 0 和 1 坑过两次。

                    四、时间格式。Jackson 全局配一次，别让每个 DTO 自己序列化。

                    五、错误码表。哪怕只有十个错误码，先写下来，比后面到处 return "参数错误" 强。

                    这五件事都是五分钟能定的，但返工成本很高。""",
                    "包结构、统一响应、分页默认值、时间格式、错误码表，起项目前先定下来",
                    "Spring Boot,工程化,后端", null, "TEXT", 0, 5),

            // 2
            new SeedPost("carol",
                    "前端要不要上 TypeScript？我最近的取舍",
                    """
                    我的判断标准很简单：这个前端会不会活过三个月，以及会不会有第二个人来改。

                    个人玩具项目，JS 更省事，省掉类型报错的摩擦。但只要涉及接口字段前后端对齐，TS 的收益立刻显出来——后端改个字段名，JS 要到线上才发现，TS 在保存文件时就报错。

                    真正让我下决心的是给老项目加类型的过程：我把后端 DTO 手抄成 interface，抄的过程本身就在暴露问题，有三个字段后端返回的是 null 而文档写的是非空。

                    代价也要说清楚：类型是需要维护的，后端加字段前端忘了补，类型就变成谎言。要么接受手动同步，要么上 openapi 生成器，两条路都比"不写类型"贵一点，但比线上踩坑便宜。""",
                    "该不该上 TS，取决于项目寿命和协作人数，以及愿不愿意维护类型本身",
                    "TypeScript,前端,工程化", null, "TEXT", 0, 8),

            // 3
            new SeedPost("david",
                    "MySQL 慢查询排查清单（附 explain 读法）",
                    """
                    记录一下我的排查顺序，避免每次都要重新想。

                    第一步，开慢查询日志确认真的是 SQL 慢，而不是应用层在等锁。有次排查半天，最后发现是连接池只有 5 个连接。

                    第二步，拿 explain 看三列：type、rows、Extra。type 出现 ALL 就是全表扫，rows 是预估扫多少行，Extra 里出现 Using filesort 说明排序没走索引。

                    第三步，看索引到底有没有用上。最常见的失效原因：字段上套了函数、隐式类型转换（varchar 传了数字）、违反最左前缀。

                    第四步，如果 SQL 本身没毛病，看数据量。单表两千万行的时候，再好的索引也顶不住深分页，得改成游标。

                    最后一句：不要一上来就加索引。索引会拖慢写入，也会占空间，先确认慢在哪一步。""",
                    "从慢查询日志到 explain 到索引失效的四个常见原因，按顺序排查",
                    "MySQL,索引,性能", "/api/seed/cover-mysql.svg", "IMAGE", 0, 11),

            // 4
            new SeedPost("alice",
                    "虚拟线程不是银弹：几个不能用的场景",
                    """
                    JDK 21 的虚拟线程很香，一个接口里并发调五个下游，代码写得跟同步一样。但它有几个地方要小心。

                    synchronized 块里做阻塞调用会钉住载体线程。官方说的是会在某些情况下 pinning，实测下来如果阻塞发生在 synchronized 内部，平台线程被占住，并发度上不去。解法是换成 ReentrantLock。

                    第二是 ThreadLocal。虚拟线程数量可能是百万级，每个都带一份 ThreadLocal 副本，内存会很难看。池化场景还好，一请求一线程的话要留意。

                    第三是 CPU 密集型任务。虚拟线程解决的是阻塞等待的并发，计算密集的活儿它帮不上忙，反而多了调度开销。

                    最后是那句老话：虚拟线程让你能写更多并发，但不代表你该无限并发。限流该做还是要做。""",
                    "pinning、ThreadLocal 内存、CPU 密集场景，虚拟线程的三个边界",
                    "Java,虚拟线程,并发", null, "TEXT", 0, 14),

            // 5
            new SeedPost("eve",
                    "我用 pgvector 做知识库检索，recall@5 从 0.62 到 0.88",
                    """
                    做面试辅助 Agent 的时候，第一版检索效果很差，问一个问题召回的资料经常答非所问。于是做了一轮量化。

                    做法是先造一份黄金集：30 个问题，每个标注哪些段落应该被召回。然后跑 recall@K，看前 K 条里命中了几条标准答案。

                    第一版 recall@5 只有 0.62。三个改进：

                    一、分块从固定 1000 字改成按段落 + 500 字重叠。之前一刀切会把一段完整论证切成两半，向量语义直接坏掉。

                    二、加了关键词兜底。纯向量检索对专有名词很弱，用户搜 "HNSW" 可能召回一堆泛泛而谈的段落。现在是向量 + 词法双路召回再做融合。

                    三、把 embedding 模型从 768 维换成 1024 维。提升有限，但配合前两条一起作用明显。

                    改完 recall@5 到 0.88。这件事最大的收获不是数字，而是：RAG 效果必须能量化，否则你永远在凭感觉调参。""",
                    "黄金集 + recall@K 量化，分块策略、双路召回、维度调整三个改进",
                    "RAG,pgvector,检索", "/api/seed/cover-vector.svg", "IMAGE", 0, 20),

            // 6
            new SeedPost("bob",
                    "JVM Full GC 排查实录",
                    """
                    线上服务每隔两小时卡一次，每次三秒。看监控是 Full GC，堆只用了 40%，很奇怪。

                    先看 GC 日志，确认是老年代满了触发的，还是元空间。结果是元空间。原因是我们用 CGLIB 动态生成代理，每次请求都新建一个 ClassLoader 却没有释放，类元数据一直涨。

                    排查手段：jstat -gcutil 看趋势，jmap -histo:live 看对象分布，最后 jcmd 拿到类加载统计，发现某个包下的类数量有十几万。

                    修复是给动态代理加了缓存，同一个接口只生成一次。改完备数量稳定在几千。

                    记录下来给以后的自己：堆没满还 Full GC，先怀疑元空间和 System.gc()。""",
                    "堆只用了 40% 却频繁 Full GC，元空间泄漏与动态代理类缓存",
                    "JVM,GC,排查", "/api/seed/cover-jvm.svg", "IMAGE", 0, 26),

            // 7
            new SeedPost("carol",
                    "小团队做社区产品，哪些中间件可以先不上",
                    """
                    这是我做完一个社区后端之后的复盘：一上来就把 Kafka、ES、MongoDB 全排上，最后两个月都在运维而不是写业务。

                    我的建议顺序。

                    缓存可以早上，但要真的需要。帖子详情读多写少，Redis 加上就有收益。

                    消息队列可以晚。评论通知这类场景，一台机器用虚拟线程异步就够了，等真的要多实例了再上 MQ。代码里留一个 Publisher 接口，切换成本很低。

                    搜索引擎最该晚。数据量不到十万条，MySQL 的 like 加索引完全够用。上 ES 意味着多一套集群、多一份数据同步逻辑，出问题时排查成本翻倍。

                    数据库选一个就够。pgvector 能顺手做向量检索，没必要为向量再单独搭一套。

                    一句话：中间件是为了解决你已有的问题，不是预防你还不知道的问题。""",
                    "缓存早上、消息队列可晚、搜索引擎最晚，中间件按需引入的顺序",
                    "架构,中间件,取舍", "/api/seed/cover-arch.svg", "IMAGE", 0, 32),

            // 8
            new SeedPost("david",
                    "Spring AI 接入大模型：同步、流式、结构化输出怎么选",
                    """
                    三种调用方式我都在项目里用过，说一下各自适合什么。

                    同步调用最简单，ChatClient.call() 一行拿结果。适合后台任务，比如给帖子生成摘要——用户不盯着看，等两秒无所谓。

                    流式输出用 stream()，返回 Flux。适合对话场景，首字延迟决定了用户觉得你"快不快"。注意流式接口要做背压和超时，模型卡住时前端不能一直转圈。

                    结构化输出用 entity(Class)，底层靠提示词约束 JSON 格式。这里有个坑：模型不是每次都听话，返回的 JSON 可能少字段或者多一层包裹。我的做法是加一层校验，解析失败就降级成规则结果，不让整个接口挂掉。

                    最后一条经验：把模型调用当成一个随时会失败的外部服务对待，超时、重试、降级三件套一个都不能省。""",
                    "同步适合后台任务、流式决定对话体感、结构化输出必须配校验与降级",
                    "Spring AI,大模型,工程化", "/api/seed/cover-spring.svg", "IMAGE", 0, 38),

            // 9
            new SeedPost("alice",
                    "还没想清楚的技术判断（私密草稿）",
                    """
                    这篇不打算公开，先把几个还没结论的想法写下来。

                    关于要不要把 Agent 做成多智能体。现在的编排是一个主循环调工具，简单可调试。但遇到需要"先规划再执行"的任务时，单循环容易在中途迷失。多智能体听上去很美，代价是调试难度指数上升——出了问题你不知道是哪个 Agent 的判断错了。

                    关于 RAG 的上限。我越来越觉得检索质量的天花板不在向量模型，而在原始资料的整理质量。垃圾进垃圾出，这话在 RAG 上格外成立。

                    关于要不要继续写博客。写的过程确实帮我想清楚很多事，但时间成本真实存在。先保持这个节奏看看。""",
                    "多智能体、RAG 上限、写作节奏，三个还没结论的判断",
                    "随笔,Agent,RAG", null, "TEXT", 1, 40),

            // 10
            new SeedPost("eve",
                    "面试被问「分布式锁怎么实现」，这样答比较稳",
                    """
                    面试官问这个，想听的不是"用 Redis 的 setnx"，而是你会不会想到边界情况。

                    我的答法分三层。

                    第一层，最小实现：SET key value NX PX 30000，一条命令原子地占锁并带过期时间。不要用 setnx + expire 两条命令，中间挂掉就死锁。

                    第二层，谁能释放。value 存一个随机 UUID，删之前比对，保证只删自己加的锁。比对和删除要用 Lua 保证原子。

                    第三层，锁过期了业务还没跑完怎么办。方案是看门狗续期，或者干脆把业务做成幂等，允许重复执行。

                    如果面试官继续问，可以聊 RedLock 的争议，以及为什么更多时候应该用数据库唯一索引或乐观锁来做，而不是硬上分布式锁。

                    核心是让对方看到你在考虑失败路径，而不是背了一个 API。""",
                    "最小实现、释放校验、过期续期三层答法，以及什么场景不该用分布式锁",
                    "面试,分布式锁,Redis", null, "TEXT", 0, 44),

            // 11
            new SeedPost("bob",
                    "记一次线上 502：不是网关的锅",
                    """
                    现象是所有接口间歇性 502，重启就好，过几小时又出现。

                    第一反应是网关超时，把超时从 5s 调到 30s，没用。看网关日志，上游返回的是 connection reset，说明请求发出去了但连接被对端断掉。

                    查后端，发现文件句柄数逼近上限。再顺着看，是上传接口的流没有关闭——我在 try 里 open 了 InputStream，异常分支直接 return，没走 finally。

                    修完句柄数稳定了。这件事的教训是：502 只说明"网关到上游这一段断了"，断的原因可能是后端自己崩了，不要只盯着网关配置。

                    顺手加了个监控，文件句柄使用率超过 70% 就告警。""",
                    "502 不等于网关问题，连接被对端断开背后是文件句柄泄漏",
                    "排查,线上问题,网络", null, "TEXT", 0, 50),

            // 12
            new SeedPost("carol",
                    "用 300 行写一个能跑的 RAG，比调框架更容易理解",
                    """
                    我不反对用框架，但建议先用最原始的方式实现一遍。

                    我写的那版只有四步。第一，把文档按段落切开，带重叠。第二，调 embedding 接口把每块转成向量，存到数组里。第三，用户提问时也转成向量，算余弦相似度，取前 K。第四，把这几块拼进提示词，发给模型。

                    整个过程没有任何抽象，300 行不到。但写完你就知道框架里那些名词对应的是什么：chunk size 对应切块长度，overlap 对应重叠，rerank 对应取完前 K 之后再过一遍排序模型。

                    之后再去看框架的配置项，就不会一脸茫然。而且当效果不好的时候，你能定位到是哪一步的问题，而不是"换个框架试试"。

                    这不是反对抽象，是反对在不理解的情况下使用抽象。""",
                    "切块、向量化、余弦召回、拼提示词，四步手写一遍就理解 RAG 了",
                    "RAG,入门,实现", null, "TEXT", 0, 56),

            // 13
            new SeedPost("david",
                    "数据库索引不是越多越好：一个反例",
                    """
                    接手一个项目，发现 t_order 上有十一个索引。问为什么，答"查询多，都加上保险"。

                    结果是写入慢得离谱。每插入一行要维护十一棵 B+ 树，批量导入一万条花了四分钟。

                    更麻烦的是优化器开始选错索引。索引太多时，优化器在某些查询上会挑一个看起来便宜但实际很差的，反而比全表扫慢。

                    清理过程：先看慢查询日志，统计每个索引被用到的次数，把三个月零命中的删掉。再把 (a) 和 (a,b) 这种前缀重复的合并。最后留了四个。

                    清理后写入快了 6 倍，查询没有任何退化。

                    经验：加索引之前先问一句"这个查询的频率是多少，值得为它维护一棵树吗"。""",
                    "十一个索引导致写入慢 6 倍，以及清理冗余索引的具体步骤",
                    "MySQL,索引,性能", null, "TEXT", 0, 62),

            // 14
            new SeedPost("admin",
                    "平台内容规范（内部草稿，暂不公开）",
                    """
                    这份是给运营同学看的内部约定，还没定稿。

                    一、标题党判定标准：标题里出现具体数字但与正文不符、使用"震惊""必看"这类强诱导词，一律降权处理。

                    二、技术类内容的边界：允许讨论具体技术方案的优劣，但不允许出现对具体公司或个人的攻击性表述。

                    三、AI 生成内容的标注：目前不强制，但如果通篇无个人实践、纯属二手转述，建议作者自己标注来源。

                    四、举报处理时效：24 小时内响应，48 小时给出结论。

                    待定：是否需要引入内容分级。这一条等社区规模再做决定。""",
                    "标题党判定、技术内容边界、AI 内容标注、举报时效的草稿约定",
                    "社区,规范,运营", null, "TEXT", 1, 68),

            // 15
            new SeedPost("eve",
                    "Spring Boot 3 升级踩坑记录",
                    """
                    把项目从 2.7 升到 3.2，花了两个下午，记录一下。

                    最直接的：javax 全换 jakarta。这个用 IDE 全局替换能解决八成，剩下两成是第三方库里还在用 javax，得等对方升级或者自己排掉。

                    Spring Security 的配置写法变了，WebSecurityConfigurerAdapter 彻底没了，改成注册 SecurityFilterChain Bean。配置逻辑其实一样，但要重写。

                    spring.sql.init 的位置从 spring.datasource.initialization-mode 挪到了 spring.sql.init.mode。这个不报错，只是静默不执行，导致本地起服务时表都没建，排查了半天。

                    Java 版本必须 17+。原本项目跑在 11 上，一起升了。

                    总体感觉：改动比想象中集中，主要是命名空间和配置项搬迁，业务代码基本没动。""",
                    "javax 换 jakarta、Security 配置重写、sql.init 配置搬家，升级的实际工作量",
                    "Spring Boot,升级,踩坑", null, "TEXT", 0, 74),

            // 16
            new SeedPost("bob",
                    "我在课程设计里踩的并发坑",
                    """
                    秒杀模块，压测时超卖了十七件。复盘一下。

                    第一版直接在 Service 里查库存、判断、扣减，三个操作之间没有锁。并发下两个请求都读到库存 1，都判断通过，都扣减。

                    第二版加 synchronized。单机测试没问题，但课堂演示要部署两台，立刻又超卖——锁只在单机有效。

                    第三版用数据库的 update ... where stock > 0，靠行锁和 where 条件保证原子。这个方案在单库下是对的，超卖没了。

                    第四版才用 Redis 预扣减 + 消息队列异步下单，把流量挡在数据库前面。

                    每一步都是被问题逼出来的，而不是一开始就设计成最终形态。我觉得这个过程比最终代码更有价值。""",
                    "从无锁到 synchronized 到行锁到 Redis 预扣，四版演进与被逼出来的设计",
                    "并发,秒杀,踩坑", null, "TEXT", 0, 80),

            // 17
            new SeedPost("carol",
                    "深夜写代码的效率，可能只是错觉",
                    """
                    我以前很迷信深夜写代码，觉得安静、思路顺。直到有次回看自己凌晨两点写的代码。

                    变量名是 a1、tmp、data2。一个函数八十行，中间嵌套三层 if。注释写着"这里先这样，明天优化"——而那个明天是两周后。

                    后来我做了个粗糙的对比：同样一个功能，下午写和凌晨写各一次，记录改动次数。凌晨写的版本，第二天返工了六次；下午那次返工两次。

                    不是说深夜不能写，而是深夜适合做不需要判断的事：跑测试、整理文档、看代码。需要设计判断的活儿，放到脑子清醒的时候。

                    现在我的做法是晚上只列第二天的任务清单，不动手写。""",
                    "深夜写的代码返工率更高，适合做体力活不适合做设计判断",
                    "随笔,效率,习惯", null, "TEXT", 0, 86)
    );

    static final List<SeedComment> COMMENTS = List.of(
            new SeedComment("bob", 0, "第三个坑我也踩过，序列化失败抛异常这个太隐蔽了，感谢提醒。", 2),
            new SeedComment("carol", 0, "补充一点：空值标记的 TTL 别设太长，否则数据真建好了还要等它过期。", 2),
            new SeedComment("admin", 0, "写得清楚，建议再补一下压测的具体参数。", 1),
            new SeedComment("alice", 1, "第二点深有体会，统一响应结构没定，后面每加一个接口都要纠结一次。", 4),
            new SeedComment("eve", 1, "分页从 0 还是 1 开始，这个真的被坑过。", 3),
            new SeedComment("david", 3, "第四步深有同感，深分页改游标之后 P99 掉了 80%。", 9),
            new SeedComment("alice", 3, "explain 里 type=index 也要留意，有些人以为走了索引就没事，其实是扫全索引树。", 8),
            new SeedComment("bob", 5, "recall@K 这个做法很实在，比「感觉准了」强太多。", 18),
            new SeedComment("carol", 5, "关键词兜底那一段很关键，纯向量对术语确实弱。", 16),
            new SeedComment("david", 7, "同意 ES 最该晚，我们就是过早上了，现在同步逻辑维护得很痛苦。", 30),
            new SeedComment("eve", 7, "中间件为了解决已有问题这句，值得贴在墙上。", 28),
            new SeedComment("alice", 8, "流式那块建议补充一下前端断连时后端怎么处理，我最近在踩这个坑。", 36),
            new SeedComment("bob", 10, "第三层答得好，很多人根本想不到锁过期这件事。", 42),
            new SeedComment("carol", 11, "句柄泄漏我们也遇到过，最后是靠加监控才发现的。", 48),
            new SeedComment("alice", 16, "四版演进的过程比直接给最优解有教学价值多了。", 78)
    );

    static final List<SeedLikes> LIKES = List.of(
            new SeedLikes(0, "bob", "carol", "david", "eve", "admin"),
            new SeedLikes(1, "alice", "carol", "eve"),
            new SeedLikes(3, "alice", "bob", "carol", "eve"),
            new SeedLikes(5, "alice", "bob", "carol", "david", "admin"),
            new SeedLikes(6, "alice", "david"),
            new SeedLikes(7, "alice", "bob", "david", "eve"),
            new SeedLikes(8, "alice", "bob", "carol"),
            new SeedLikes(10, "alice", "bob", "carol", "david"),
            new SeedLikes(12, "bob", "david", "eve"),
            new SeedLikes(13, "alice", "carol"),
            new SeedLikes(16, "alice", "carol", "david", "eve")
    );

    static final List<SeedFavorite> FAVORITES = List.of(
            new SeedFavorite("alice", 3, 10),
            new SeedFavorite("alice", 5, 18),
            new SeedFavorite("alice", 12, 54),
            new SeedFavorite("bob", 0, 2),
            new SeedFavorite("bob", 8, 36),
            new SeedFavorite("admin", 0, 2),
            new SeedFavorite("admin", 7, 30),
            new SeedFavorite("carol", 3, 8)
    );

    static final List<SeedFollow> FOLLOWS = List.of(
            new SeedFollow("alice", "bob"),
            new SeedFollow("alice", "carol"),
            new SeedFollow("alice", "eve"),
            new SeedFollow("bob", "alice"),
            new SeedFollow("carol", "alice"),
            new SeedFollow("david", "alice"),
            new SeedFollow("eve", "alice"),
            new SeedFollow("eve", "bob"),
            new SeedFollow("admin", "alice")
    );
}
