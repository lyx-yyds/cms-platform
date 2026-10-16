package com.cms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问日志（sys_visit_log，文档 4.2.12；统计口径见 7.11）
 */
@Data
@TableName("sys_visit_log")
public class VisitLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String ipAddress;

    /** 浏览器 UA */
    private String userAgent;

    private String requestUrl;

    private String requestMethod;

    /** 响应时间（ms） */
    private Integer responseTime;

    private LocalDateTime createTime;
}
