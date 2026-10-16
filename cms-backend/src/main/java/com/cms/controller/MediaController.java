package com.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.Result;
import com.cms.dto.response.MediaResp;
import com.cms.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体接口（controller/MediaController.java，文档 5.1）
 * 上传/列表/删除为创作角色可用（ADMIN/EDITOR/AUTHOR）；删除为数据级权限（本人或 ADMIN，文档 7.10）
 */
@Tag(name = "媒体")
@RestController("mediaController")
@RequestMapping("/api/media")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('media:upload')")
public class MediaController {

    private final MediaService mediaService;

    @Operation(summary = "上传文件（扩展名白名单 / MIME 校验 / 大小限制，文档 7.8）")
    @PostMapping("/upload")
    public Result<MediaResp> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(mediaService.upload(file));
    }

    @Operation(summary = "文件分页列表")
    @GetMapping
    public Result<Page<MediaResp>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return Result.ok(mediaService.pageMedia(page, size, type, keyword));
    }

    @Operation(summary = "删除文件（本人或 ADMIN）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mediaService.deleteMedia(id);
        return Result.ok();
    }
}
