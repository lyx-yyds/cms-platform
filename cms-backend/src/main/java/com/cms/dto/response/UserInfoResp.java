package com.cms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户信息响应（UserInfoResp）
 */
@Data
@Builder
public class UserInfoResp {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
}
