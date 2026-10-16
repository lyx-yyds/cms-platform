package com.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.dto.request.ForgotPasswordReq;
import com.cms.dto.request.LoginReq;
import com.cms.dto.request.RefreshTokenReq;
import com.cms.dto.request.RegisterReq;
import com.cms.dto.request.ResetPasswordReq;
import com.cms.dto.request.ChangePasswordReq;
import com.cms.dto.request.UpdateProfileReq;
import com.cms.dto.request.UserCreateReq;
import com.cms.dto.request.UserUpdateReq;
import com.cms.dto.response.LoginResp;
import com.cms.dto.response.UserInfoResp;
import com.cms.dto.response.UserPageResp;

/**
 * 用户服务（service/UserService.java）
 */
public interface UserService {

    /** 登录（双令牌，文档 7.1） */
    LoginResp login(LoginReq req);

    /** 注册（默认 USER 角色） */
    void register(RegisterReq req);

    /** 刷新 AccessToken（GETDEL 防重放，文档 7.1） */
    LoginResp refresh(RefreshTokenReq req);

    /** 退出登录（注销 RefreshToken） */
    void logout();

    /** 忘记密码：发送重置邮件（v2.1） */
    void forgotPassword(ForgotPasswordReq req);

    /** 重置密码（v2.1） */
    void resetPassword(ResetPasswordReq req);

    /** 修改密码（已登录，校验原密码，无需邮件） */
    void changePassword(ChangePasswordReq req);

    /** 当前用户信息 */
    UserInfoResp getUserInfo();

    /** 修改个人信息（昵称/邮箱/头像，部分更新） */
    void updateProfile(UpdateProfileReq req);

    // ---------- 管理端（第二阶段） ----------

    /** 分页查询用户（支持用户名/昵称模糊、状态筛选） */
    Page<UserPageResp> pageUsers(long pageNum, long pageSize, String keyword, Integer status);

    /** 用户详情（含角色） */
    UserPageResp getUserDetail(Long id);

    /** 创建用户（可指定角色） */
    void createUser(UserCreateReq req);

    /** 修改用户（可重置密码、重设角色） */
    void updateUser(Long id, UserUpdateReq req);

    /** 删除用户（禁止删除自己） */
    void deleteUser(Long id);
}
