package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色创建/修改请求（admin，RoleReq）
 */
@Data
public class RoleReq {

    @NotBlank(message = "角色名称不能为空")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    private String description;

    /** 1-正常 0-禁用 */
    private Integer status;

    /** 权限 ID 列表 */
    private List<Long> permissionIds;
}
