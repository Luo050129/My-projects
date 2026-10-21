-- 内容社区库表结构（H2 MySQL 兼容模式）
-- 表名统一加 t_ 前缀，规避 user/post/comment 等 SQL 保留字
-- 重启时 IF NOT EXISTS 保证幂等

CREATE TABLE IF NOT EXISTS t_user (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL,
    password     VARCHAR(100) NOT NULL,
    nickname     VARCHAR(64)  NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status       TINYINT      NOT NULL DEFAULT 1,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS t_post (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    title        VARCHAR(200) NOT NULL,
    content      VARCHAR(8000) NOT NULL,
    summary      VARCHAR(500),
    tags         VARCHAR(255),
    status       TINYINT      NOT NULL DEFAULT 1,
    view_count   INT          NOT NULL DEFAULT 0,
    like_count   INT          NOT NULL DEFAULT 0,
    comment_count INT         NOT NULL DEFAULT 0,
    ai_enhanced  TINYINT      NOT NULL DEFAULT 0,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_post_user ON t_post (user_id);
CREATE INDEX IF NOT EXISTS idx_post_status_ctime ON t_post (status, create_time DESC);

CREATE TABLE IF NOT EXISTS t_comment (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    post_id      BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    parent_id    BIGINT       NOT NULL DEFAULT 0,
    content      VARCHAR(500) NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 1,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_comment_post ON t_comment (post_id);

CREATE TABLE IF NOT EXISTS t_post_like (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    post_id      BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_like_post_user UNIQUE (post_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_like_post ON t_post_like (post_id);

CREATE TABLE IF NOT EXISTS t_notification (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    receiver_id  BIGINT       NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    content      VARCHAR(500) NOT NULL,
    ref_id       BIGINT,
    read_status  TINYINT      NOT NULL DEFAULT 0,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notify_receiver ON t_notification (receiver_id, read_status);
