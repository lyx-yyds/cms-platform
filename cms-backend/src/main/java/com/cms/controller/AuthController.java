package com.cms.controller;

import com.cms.common.Result;
import com.cms.dto.request.ForgotPasswordReq;
import com.cms.dto.request.LoginReq;
import com.cms.dto.request.RefreshTokenReq;
import com.cms.dto.request.RegisterReq;
import com.cms.dto.request.ResetPasswordReq;
import com.cms.dto.request.ChangePasswordReq;
import com.cms.dto.request.UpdateProfileReq;
import com.cms.dto.response.LoginResp;
import com.cms.dto.response.UserInfoResp;
import com.cms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与用户接口（controller/auth/AuthController.java，文档 5.1 接口表）
 */
@Tag(name = "认证与用户")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "登录（双令牌）")
    @PostMapping("/auth/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(userService.login(req));
    }

    @Operation(summary = "注册（默认普通用户）")
    @PostMapping("/auth/register")
    public Result<Void> register(@Valid @RequestBody RegisterReq req) {
        userService.register(req);
        return Result.ok();
    }

    @Operation(summary = "刷新 AccessToken（GETDEL 防重放）")
    @PostMapping("/auth/refresh")
    public Result<LoginResp> refresh(@Valid @RequestBody RefreshTokenReq req) {
        return Result.ok(userService.refresh(req));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/auth/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok();
    }

    @Operation(summary = "忘记密码：发送重置邮件")
    @PostMapping("/auth/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordReq req) {
        userService.forgotPassword(req);
        return Result.ok();
    }

    @Operation(summary = "重置密码（一次性令牌）")
    @PostMapping("/auth/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordReq req) {
        userService.resetPassword(req);
        return Result.ok();
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/user/info")
    public Result<UserInfoResp> getUserInfo() {
        return Result.ok(userService.getUserInfo());
    }

    @Operation(summary = "更新用户信息（昵称/头像/邮箱，部分更新）")
    @PutMapping("/user/info")
    public Result<Void> updateUserInfo(@Valid @RequestBody UpdateProfileReq req) {
        userService.updateProfile(req);
        return Result.ok();
    }

    @Operation(summary = "修改密码（登录用户，校验原密码，无需邮件）")
    @PostMapping("/user/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordReq req) {
        userService.changePassword(req);
        return Result.ok();
    }
}
