package com.cms.controller.front;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.Constants;
import com.cms.common.Result;
import com.cms.dto.request.ArticleCreateReq;
import com.cms.dto.request.ArticleUpdateReq;
import com.cms.dto.response.ArticleDetailResp;
import com.cms.dto.response.ArticleResp;
import com.cms.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章接口（front/ArticleController.java，文档 5.1）
 * 公开：GET 列表/详情；登录：创建/修改/删除/我的/点赞（数据级权限见 7.3）
 */
@Tag(name = "文章")
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "分页查询文章（前台默认只看已发布）")
    @GetMapping
    public Result<Page<ArticleResp>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        // 公开场景默认只看已发布（文档 7.2 前台展示）；传 status 则按指定状态筛选
        Integer effectiveStatus = status == null ? Constants.ARTICLE_STATUS_PUBLISHED : status;
        return Result.ok(articleService.pageArticles(page, size, keyword, categoryId, effectiveStatus, null));
    }

    @Operation(summary = "我的文章（创作角色；ADMIN 返回全部文章，数据级权限见 7.3）")
    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('article:list')")
    public Result<Page<ArticleResp>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Long authorFilter = com.cms.security.SecurityUtils.hasRole("ADMIN")
                ? null
                : com.cms.security.SecurityUtils.getCurrentUserId();
        return Result.ok(articleService.pageArticles(page, size, keyword, null, status, authorFilter));
    }

    @Operation(summary = "文章详情（浏览量 Redis 原子自增）")
    @GetMapping("/{id}")
    public Result<ArticleDetailResp> detail(@PathVariable Long id) {
        return Result.ok(articleService.getArticleDetail(id));
    }

    @Operation(summary = "创建文章（创作角色：ADMIN/EDITOR/AUTHOR）")
    @PostMapping
    @PreAuthorize("hasAuthority('article:create')")
    public Result<Long> create(@Valid @RequestBody ArticleCreateReq req) {
        return Result.ok(articleService.createArticle(req));
    }

    @Operation(summary = "修改文章（仅作者或 ADMIN；创作角色）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('article:update')")
    public Result<Void> update(@PathVariable Long id, @RequestBody ArticleUpdateReq req) {
        articleService.updateArticle(id, req);
        return Result.ok();
    }

    @Operation(summary = "删除文章（逻辑删除，仅作者或 ADMIN；创作角色）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('article:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.ok();
    }

    @Operation(summary = "点赞（SADD 幂等）")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        articleService.likeArticle(id);
        return Result.ok();
    }

    @Operation(summary = "取消点赞（SREM）")
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable Long id) {
        articleService.unlikeArticle(id);
        return Result.ok();
    }
}
