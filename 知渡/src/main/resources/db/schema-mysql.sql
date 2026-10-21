-- ============================================================
-- 内容社区库表结构（MySQL 8 / InnoDB / utf8mb4）
-- 与 db/schema-h2.sql 一一对应：字段、索引、约束保持一致，仅按 MySQL 语法调整。
--
-- 使用方式（生产 spring.sql.init.mode=never，由人工或迁移工具执行）：
--   1) CREATE DATABASE community DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
--   2) mysql -u root -p community < src/main/resources/db/schema-mysql.sql
--
-- 说明：
--   1. 建表用 CREATE TABLE IF NOT EXISTS，重复执行安全；
--   2. 索引与唯一键全部内联在 CREATE TABLE 中声明：表已存在时整句跳过，脚本整体可重复执行。
--      （MySQL 的 CREATE INDEX 不支持 IF NOT EXISTS，故刻意不采用独立 CREATE INDEX 写法）
--   3. 表名统一 t_ 前缀，规避 user / post / comment 等保留字。
-- ============================================================

CREATE TABLE IF NOT EXISTS t_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username     VARCHAR(64)  NOT NULL                COMMENT '用户名',
    password     VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密文',
    nickname     VARCHAR(64)  NOT NULL                COMMENT '昵称',
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    status       TINYINT      NOT NULL DEFAULT 1      COMMENT '1 正常 0 禁用',
    deleted      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS t_post (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT        NOT NULL                COMMENT '作者 id',
    title         VARCHAR(200)  NOT NULL                COMMENT '标题',
    content       VARCHAR(8000) NOT NULL                COMMENT '正文',
    summary       VARCHAR(500)  NULL                    COMMENT 'AI 摘要',
    tags          VARCHAR(255)  NULL                    COMMENT '标签，逗号分隔',
    status        TINYINT       NOT NULL DEFAULT 1      COMMENT '1 已发布 0 草稿',
    view_count    INT           NOT NULL DEFAULT 0      COMMENT '浏览量',
    like_count    INT           NOT NULL DEFAULT 0      COMMENT '点赞数',
    comment_count INT           NOT NULL DEFAULT 0      COMMENT '评论数',
    ai_enhanced   TINYINT       NOT NULL DEFAULT 0      COMMENT '是否经过 AI 增强',
    deleted       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_post_user (user_id),
    KEY idx_post_status_ctime (status, create_time DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '帖子表';

CREATE TABLE IF NOT EXISTS t_comment (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    post_id     BIGINT       NOT NULL                COMMENT '所属帖子',
    user_id     BIGINT       NOT NULL                COMMENT '评论人',
    parent_id   BIGINT       NOT NULL DEFAULT 0      COMMENT '父评论 id，0 为顶层',
    content     VARCHAR(500) NOT NULL                COMMENT '评论内容',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '1 正常 0 屏蔽',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_comment_post (post_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论表';

CREATE TABLE IF NOT EXISTS t_post_like (
    id          BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
    post_id     BIGINT  NOT NULL                COMMENT '帖子 id',
    user_id     BIGINT  NOT NULL                COMMENT '点赞人 id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_like_post_user (post_id, user_id) COMMENT '唯一索引兜底点赞幂等',
    KEY idx_like_post (post_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '帖子点赞表';

CREATE TABLE IF NOT EXISTS t_notification (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    receiver_id BIGINT       NOT NULL                COMMENT '接收人 id',
    type        VARCHAR(20)  NOT NULL                COMMENT '通知类型',
    content     VARCHAR(500) NOT NULL                COMMENT '通知内容',
    ref_id      BIGINT       NULL                    COMMENT '关联业务 id',
    read_status TINYINT      NOT NULL DEFAULT 0      COMMENT '0 未读 1 已读',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notify_receiver (receiver_id, read_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知表';

