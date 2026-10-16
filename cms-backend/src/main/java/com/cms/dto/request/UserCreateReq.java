package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建用户请求（admin，UserCreateReq）
 */
@Data
public class UserCreateReq {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度 3-50 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64 位")
    private String password;

    private String nickname;

    private String email;

    private String phone;

    /** 1-正常 0-禁用 */
    private Integer status;

    /** 角色 ID 列表（为空则默认普通用户） */
    private List<Long> roleIds;
}
