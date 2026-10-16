package com.cms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 媒体文件（cms_media，文档 4.2.11）
 */
@Data
@TableName("cms_media")
public class Media {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文件名（服务端生成） */
    private String name;

    /** 原始文件名 */
    private String originalName;

    /** 文件 URL */
    private String url;

    /** 文件类型（MIME） */
    private String type;

    /** 文件大小（字节） */
    private Long size;

    /** 文件扩展名 */
    private String extension;

    /** 上传用户ID */
    private Long uploadUserId;

    private LocalDateTime createTime;
}
