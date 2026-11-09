-- ============================================================
-- 内容可信治理：AI 生成标识 + 审核状态机 + 举报申诉 + 审计留痕
--
-- 依据：《人工智能生成合成内容标识办法》与 GB 45438—2025（2025-09-01 施行）
--   显式标识：PostVO.aiGenerated 由前端展示「AI 生成 / 辅助创作」；
--   隐式标识：t_post.ai_declaration 存结构化声明元数据（声明人、时间、来源）；
--   标识只解决「来源可识别」，不等于真实性认证，因此另设审核状态机与申诉通道。
--
-- review_status：0 免审（历史存量与规则放行）/ 1 待审 / 2 审核通过 / 3 驳回
-- t_content_review.status：0 待审 / 1 通过 / 2 驳回 / 3 申诉中 / 4 申诉通过 / 5 维持原判
-- t_report.status：0 待处理 / 1 成立 / 2 不成立
-- ============================================================

ALTER TABLE t_post ADD COLUMN ai_generated TINYINT NOT NULL DEFAULT 0;
ALTER TABLE t_post ADD COLUMN review_status TINYINT NOT NULL DEFAULT 0;
ALTER TABLE t_post ADD COLUMN ai_declaration VARCHAR(1000);
CREATE INDEX idx_post_review ON t_post (review_status, create_time);

CREATE TABLE t_content_review (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    post_id       BIGINT       NOT NULL,
    submitter_id  BIGINT       NOT NULL,
    source        VARCHAR(20)  NOT NULL,
    reason        VARCHAR(500),
    status        TINYINT      NOT NULL DEFAULT 0,
    reviewer_id   BIGINT,
    review_note   VARCHAR(500),
    appeal_text   VARCHAR(500),
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    review_time   DATETIME,
    PRIMARY KEY (id)
);
CREATE INDEX idx_review_status ON t_content_review (status, create_time);
CREATE INDEX idx_review_post ON t_content_review (post_id);

CREATE TABLE t_report (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    post_id       BIGINT       NOT NULL,
    reporter_id   BIGINT       NOT NULL,
    reason        VARCHAR(500) NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 0,
    handler_id    BIGINT,
    handle_note   VARCHAR(500),
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    handle_time   DATETIME,
    PRIMARY KEY (id)
);
CREATE INDEX idx_report_status ON t_report (status, create_time);
CREATE INDEX idx_report_post ON t_report (post_id);

-- 所有治理动作统一留痕：谁、在何时、把哪个业务对象从什么状态改到什么状态
CREATE TABLE t_moderation_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    biz_type      VARCHAR(20)  NOT NULL,
    biz_id        BIGINT       NOT NULL,
    post_id       BIGINT       NOT NULL,
    action        VARCHAR(30)  NOT NULL,
    operator_id   BIGINT,
    from_status   TINYINT,
    to_status     TINYINT,
    note          VARCHAR(500),
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_audit_biz ON t_moderation_log (biz_type, biz_id);
CREATE INDEX idx_audit_post ON t_moderation_log (post_id);
