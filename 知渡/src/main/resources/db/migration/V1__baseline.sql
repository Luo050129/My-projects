-- ============================================================
-- 知渡 baseline：内容与用户域初始结构
--
-- 单份脚本同时兼容 H2（MySQL 模式）与 MySQL 8：
--   1. 只用两种方言都支持的 DDL：内联 PRIMARY KEY / 表级 UNIQUE 约束 /
--      独立 CREATE INDEX / AUTO_INCREMENT / TINYINT / DATETIME；
--   2. 刻意不用 CREATE INDEX IF NOT EXISTS（MySQL 不支持）、
--      ENGINE / CHARSET / COMMENT / ON UPDATE CURRENT_TIMESTAMP（H2 不支持）；
--   3. 索引不带 DESC（H2 对降序索引支持不一致），由优化器反向扫描。
-- Flyway 保证每个版本只执行一次，因此无需 IF NOT EXISTS 幂等。
--
-- 对已存在的老库（如 data/community.mv.db）启用 baseline-on-migrate，
-- 本版本会被标记为已基线而不重复执行；后续 V2+ 正常叠加。
-- ============================================================

CREATE TABLE t_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(64)  NOT NULL,
    password     VARCHAR(100) NOT NULL,
    nickname     VARCHAR(64)  NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status       TINYINT      NOT NULL DEFAULT 1,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE t_post (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    content       VARCHAR(8000) NOT NULL,
    summary       VARCHAR(500),
    tags          VARCHAR(255),
    cover_image   VARCHAR(255),
    post_type     VARCHAR(20)   NOT NULL DEFAULT 'TEXT',
    visibility    TINYINT       NOT NULL DEFAULT 0,
    status        TINYINT       NOT NULL DEFAULT 1,
    view_count    INT           NOT NULL DEFAULT 0,
    like_count    INT           NOT NULL DEFAULT 0,
    comment_count INT           NOT NULL DEFAULT 0,
    ai_enhanced   TINYINT       NOT NULL DEFAULT 0,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_post_user ON t_post (user_id);
CREATE INDEX idx_post_status_ctime ON t_post (status, create_time);
CREATE INDEX idx_post_visibility_ctime ON t_post (visibility, create_time);

CREATE TABLE t_comment (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    post_id     BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    content     VARCHAR(500) NOT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_comment_post ON t_comment (post_id);

CREATE TABLE t_post_like (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    post_id     BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_like_post_user UNIQUE (post_id, user_id)
);
CREATE INDEX idx_like_post ON t_post_like (post_id);

CREATE TABLE t_notification (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    receiver_id  BIGINT       NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    content      VARCHAR(500) NOT NULL,
    ref_id       BIGINT,
    read_status  TINYINT      NOT NULL DEFAULT 0,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_notify_receiver ON t_notification (receiver_id, read_status);

CREATE TABLE t_user_follow (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    follower_id  BIGINT   NOT NULL,
    followee_id  BIGINT   NOT NULL,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_follow_pair UNIQUE (follower_id, followee_id)
);
CREATE INDEX idx_follow_followee ON t_user_follow (followee_id);

CREATE TABLE t_post_favorite (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    post_id     BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_favorite_post_user UNIQUE (post_id, user_id)
);
CREATE INDEX idx_favorite_user ON t_post_favorite (user_id);
