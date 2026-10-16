package com.cms.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 标签响应（TagResp）
 */
@Data
@Builder
public class TagResp {

    private Long id;
    private String name;
    private String slug;
    /** 标签下文章数 */
    private Long articleCount;
}
