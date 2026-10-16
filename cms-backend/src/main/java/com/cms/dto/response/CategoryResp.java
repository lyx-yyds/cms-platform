package com.cms.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 分类响应（CategoryResp）
 */
@Data
@Builder
public class CategoryResp {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private Integer sortOrder;
    private Integer status;
    /** 分类下文章数 */
    private Long articleCount;
}
