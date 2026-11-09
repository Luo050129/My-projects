package com.luoxianjun.community.module.governance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户举报单。举报与内容审核解耦：举报只登记，处置由管理员在审核侧完成。 */
@Data
@TableName("t_report")
public class Report {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_VALID = 1;
    public static final int STATUS_INVALID = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long reporterId;
    private String reason;
    private Integer status;
    private Long handlerId;
    private String handleNote;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
