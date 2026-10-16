package com.cms.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应（LoginResp，含双令牌，v2.0）
 */
@Data
@Builder
public class LoginResp {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}
