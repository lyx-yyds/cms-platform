package com.cms.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 修改用户请求（admin，UserUpdateReq）
 */
@Data
public class UserUpdateReq {

    private String nickname;

    private String email;

    private String phone;

    /** 1-正常 0-禁用 */
    private Integer status;

    /** 可选：重置密码（不传则不修改） */
    @Size(min = 6, max = 64, message = "密码长度 6-64 位")
    private String password;

    /** 角色 ID 列表（为空则清空角色） */
    private List<Long> roleIds;
}
