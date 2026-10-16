package com.cms.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 媒体文件响应（文档 5.1）
 */
@Data
public class MediaResp {

    private Long id;
    private String name;
    private String originalName;
    private String url;
    private String type;
    private Long size;
    private String extension;
    private Long uploadUserId;
    private String uploadUserNickname;
    private LocalDateTime createTime;
}
