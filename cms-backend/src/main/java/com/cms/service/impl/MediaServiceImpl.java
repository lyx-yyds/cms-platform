package com.cms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.BusinessException;
import com.cms.common.ResultCode;
import com.cms.dto.response.MediaResp;
import com.cms.entity.Media;
import com.cms.mapper.MediaMapper;
import com.cms.security.SecurityUtils;
import com.cms.service.MediaService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 媒体服务实现（文档 5.1 / 7.8 上传安全 / 7.10 删除策略）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaMapper mediaMapper;
    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /** 扩展名白名单（文档 7.8：jpg/png/gif/webp/pdf/docx/xlsx 等） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "docx", "xlsx");

    /** 图片类扩展名（≤5MB），其余文档类（≤20MB） */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private static final long IMAGE_MAX_SIZE = 5L * 1024 * 1024;    // 图片 ≤ 5MB
    private static final long DOC_MAX_SIZE = 20L * 1024 * 1024;     // 文档 ≤ 20MB

    @Override
    @Transactional
    public MediaResp upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        // 1. 扩展名白名单（cleanPath 防路径穿越）
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "不支持的文件类型 ." + ext + "，允许：jpg/jpeg/png/gif/webp/pdf/docx/xlsx");
        }
        // 2. 大小限制（文档 7.8：图片 ≤5MB，文档 ≤20MB）
        long size = file.getSize();
        long limit = IMAGE_EXTENSIONS.contains(ext) ? IMAGE_MAX_SIZE : DOC_MAX_SIZE;
        if (size > limit) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "文件大小超过限制（" + (limit / 1024 / 1024) + "MB）");
        }
        // 3. MIME magic bytes 二次校验（不以扩展名信任，文档 7.8）
        byte[] head;
        try {
            head = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件读取失败");
        }
        if (!matchesMagic(ext, head)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件内容与扩展名不符，已拦截");
        }
        // 4. 服务端随机文件名（UUID，防路径穿越与重名覆盖），按月份分目录
        String objectKey = "media/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(head), head.length, -1)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("MinIO 上传失败 objectKey={}", objectKey, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传失败，请检查 MinIO 服务");
        }
        // 5. 落库
        Media media = new Media();
        media.setName(objectKey);
        media.setOriginalName(originalName);
        media.setUrl(endpoint + "/" + bucketName + "/" + objectKey);
        media.setType(file.getContentType());
        media.setSize(size);
        media.setExtension(ext);
        media.setUploadUserId(SecurityUtils.getCurrentUserId());
        mediaMapper.insert(media);
        return toResp(media, null);
    }

    @Override
    public Page<MediaResp> pageMedia(int page, int size, String type, String keyword) {
        Page<MediaResp> result = mediaMapper.selectMediaPage(new Page<>(page, size), type, keyword);
        return result;
    }

    @Override
    @Transactional
    public void deleteMedia(Long id) {
        Media media = mediaMapper.selectById(id);
        if (media == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        Long current = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        if (!isAdmin && !current.equals(media.getUploadUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己上传的文件");
        }
        // 文档 7.10：先删数据库记录，再删 MinIO 对象（避免先删对象后记录删除失败导致引用断裂）
        mediaMapper.deleteById(id);
        String objectKey = media.getName();
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectKey).build());
        } catch (Exception e) {
            // MinIO 删除失败：记录待清理（WARN 留痕），避免阻塞主流程；对象残留可由运维清理
            log.warn("MinIO 对象删除失败，存在孤儿文件风险 objectKey={}: {}", objectKey, e.getMessage());
        }
    }

    private String extensionOf(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /** 文件头 magic bytes 校验（与扩展名强绑定，防止伪造扩展名） */
    private boolean matchesMagic(String ext, byte[] head) {
        if (head.length < 12) {
            return false;
        }
        return switch (ext) {
            case "jpg", "jpeg" -> (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case "png" -> (head[0] & 0xFF) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47;
            case "gif" -> head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8';
            case "webp" -> head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
            case "pdf" -> head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
            case "docx", "xlsx" -> head[0] == 'P' && head[1] == 'K' && (head[2] == 0x03 || head[2] == 0x05 || head[2] == 0x07);
            default -> false;
        };
    }

    private MediaResp toResp(Media media, String nickname) {
        MediaResp resp = new MediaResp();
        resp.setId(media.getId());
        resp.setName(media.getName());
        resp.setOriginalName(media.getOriginalName());
        resp.setUrl(media.getUrl());
        resp.setType(media.getType());
        resp.setSize(media.getSize());
        resp.setExtension(media.getExtension());
        resp.setUploadUserId(media.getUploadUserId());
        resp.setUploadUserNickname(nickname);
        resp.setCreateTime(media.getCreateTime());
        return resp;
    }
}
