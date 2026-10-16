package com.cms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理列表项（UserPageResp）
 */
@Data
@Builder
public class UserPageResp {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String phone;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private List<String> roles;
}
