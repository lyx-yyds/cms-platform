package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 标签创建/修改请求（TagReq）
 */
@Data
public class TagReq {

    @NotBlank(message = "标签名称不能为空")
    private String name;

    private String slug;
}
