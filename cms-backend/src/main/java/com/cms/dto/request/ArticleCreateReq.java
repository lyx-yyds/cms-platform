package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建文章请求（ArticleCreateReq，文档 7.2 发布流程）
 */
@Data
public class ArticleCreateReq {

    @NotBlank(message = "文章标题不能为空")
    private String title;

    /** 富文本 HTML（入库前 JSoup 白名单清洗，见 7.8） */
    private String content;

    private String summary;

    private String coverImage;

    private Long categoryId;

    /** 0-草稿 1-已发布 2-已下架 */
    private Integer status;

    private Boolean isTop;

    private Boolean isRecommend;

    private List<Long> tagIds;
}
