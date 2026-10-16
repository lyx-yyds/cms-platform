package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 Token 请求（RefreshTokenReq，v2.0）
 */
@Data
public class RefreshTokenReq {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
