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
    cover_image  VARCHAR(255),
    post_type    VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    visibility   TINYINT      NOT NULL DEFAULT 0,
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
-- 注意：visibility 的索引不能放这里。老库此时还没有这一列，
-- 建索引会直接报 Column "visibility" not found，所以挪到文件末尾的 ALTER 之后。

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

-- 关注关系：follower 关注 followee。唯一索引保证重复关注幂等，不会产生脏数据。
CREATE TABLE IF NOT EXISTS t_user_follow (
    id           BIGINT   AUTO_INCREMENT PRIMARY KEY,
    follower_id  BIGINT   NOT NULL,
    followee_id  BIGINT   NOT NULL,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_follow_pair UNIQUE (follower_id, followee_id)
);
CREATE INDEX IF NOT EXISTS idx_follow_followee ON t_user_follow (followee_id);

-- 帖子收藏：与点赞同构，唯一索引保证一人一收藏。
CREATE TABLE IF NOT EXISTS t_post_favorite (
    id           BIGINT   AUTO_INCREMENT PRIMARY KEY,
    post_id      BIGINT   NOT NULL,
    user_id      BIGINT   NOT NULL,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorite_post_user UNIQUE (post_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_favorite_user ON t_post_favorite (user_id);

-- ------------------------------------------------------------
-- 增量迁移：老库补字段
-- CREATE TABLE IF NOT EXISTS 对新库有效，但已存在的表不会加列，
-- 直接依赖上面的建表语句会导致老库启动后插入报「列不存在」。
-- H2 支持 ADD COLUMN IF NOT EXISTS，重复执行无副作用。
-- MySQL 8 不支持该语法，生产环境请人工执行对应 ALTER（见 schema-mysql.sql 文件头）。
-- ------------------------------------------------------------
ALTER TABLE t_post ADD COLUMN IF NOT EXISTS cover_image VARCHAR(255);

-- 帖子形态与可见性：post_type 区分纯文字 / 图文，visibility 控制是否私密。
-- 私密帖只对作者本人可见，列表与详情都会做过滤（详情由服务层校验，不依赖前端隐藏）。
ALTER TABLE t_post ADD COLUMN IF NOT EXISTS post_type VARCHAR(20) NOT NULL DEFAULT 'TEXT';
ALTER TABLE t_post ADD COLUMN IF NOT EXISTS visibility TINYINT NOT NULL DEFAULT 0;

-- 列补齐之后才能建索引（老库直接建会报列不存在）
CREATE INDEX IF NOT EXISTS idx_post_visibility_ctime ON t_post (visibility, create_time DESC);
