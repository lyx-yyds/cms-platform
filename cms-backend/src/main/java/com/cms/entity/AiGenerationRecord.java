package com.cms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 生成记录（ai_generation_record 表，4.2.13；文档 7.4 成本审计）
 */
@Data
@TableName("ai_generation_record")
public class AiGenerationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long articleId;

    /** 生成类型：draft草稿/continue续写/polish润色/summary摘要/seo */
    private String type;

    private String prompt;

    private String result;

    private String model;

    /** 0-处理中 1-成功 2-失败 */
    private Integer status;

    /** 消耗 Token 数（成本审计） */
    private Integer tokensUsed;

    private String errorMsg;

    /** 所属用户昵称（非表字段，管理端全量视图填充） */
    @TableField(exist = false)
    private String username;

    private LocalDateTime createTime;
}
