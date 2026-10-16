package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分类创建/修改请求（CategoryReq）
 */
@Data
public class CategoryReq {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String slug;

    private String description;

    private Long parentId;

    private Integer sortOrder;

    /** 1-正常 0-禁用 */
    private Integer status;
}
