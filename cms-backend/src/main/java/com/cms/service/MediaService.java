package com.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.dto.response.MediaResp;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体服务（文档 5.1 / 7.8 / 7.10）
 */
public interface MediaService {

    /** 上传文件（白名单 / MIME magic bytes 校验 / 大小限制 / 服务端随机文件名，文档 7.8） */
    MediaResp upload(MultipartFile file);

    /** 文件分页列表（登录可见） */
    Page<MediaResp> pageMedia(int page, int size, String type, String keyword);

    /** 删除文件（本人或 ADMIN；先删 DB 记录，再删 MinIO 对象，文档 7.10） */
    void deleteMedia(Long id);
}
