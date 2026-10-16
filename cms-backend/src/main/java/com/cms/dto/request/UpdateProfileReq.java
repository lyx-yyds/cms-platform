package com.cms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人信息请求（PUT /api/user/info，接口总览 5.1 用户接口）
 * 昵称 / 邮箱 / 头像均可选，只更新传入的非空字段
 */
@Data
public class UpdateProfileReq {

    @Size(max = 50, message = "昵称长度不超过 50 位")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不超过 100 位")
    private String email;

    @Size(max = 500, message = "头像地址过长")
    private String avatar;
}
