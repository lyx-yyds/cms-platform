package com.cms.controller.front;

import com.cms.common.Result;
import com.cms.dto.response.TagResp;
import com.cms.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台标签接口（公开）
 */
@Tag(name = "前台-标签")
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "标签列表（公开）")
    @GetMapping
    public Result<List<TagResp>> list() {
        return Result.ok(tagService.listTags());
    }
}
