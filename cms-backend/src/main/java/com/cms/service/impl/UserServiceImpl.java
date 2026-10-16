package com.cms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.BusinessException;
import com.cms.common.Constants;
import com.cms.common.ResultCode;
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
import com.cms.entity.User;
import com.cms.mapper.UserMapper;
import com.cms.security.JwtTokenProvider;
import com.cms.security.LoginUser;
import com.cms.security.SecurityUtils;
import com.cms.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户服务实现（service/impl/UserServiceImpl.java）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String REFRESH_KEY_PREFIX = "refresh:token:";

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender javaMailSender;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${spring.mail.host}")
    private String mailHost;

    /** 登录 */
    @Override
    public LoginResp login(LoginReq req) {
        // 1. Spring Security 认证（用户名/密码）；密码错误/账号禁用需转业务码，避免被全局处理为 401"未登录"
        LoginUser loginUser;
        try {
            loginUser = (LoginUser) authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            ).getPrincipal();
        } catch (DisabledException e) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        } catch (AuthenticationException e) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 2. 签发双令牌
        String accessToken = jwtTokenProvider.generateAccessToken(loginUser.getUserId(), loginUser.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(loginUser.getUserId(), loginUser.getUsername());

        // 3. RefreshToken 存入 Redis（key: refresh:token:{userId}:{jti}，文档 7.1）
        String jti = jwtTokenProvider.getJti(refreshToken);
        stringRedisTemplate.opsForValue().set(
                redisRefreshKey(loginUser.getUserId(), jti),
                loginUser.getUsername(),
                Duration.ofMillis(refreshTokenExpiration));

        // 4. 更新最后登录时间
        User update = new User();
        update.setId(loginUser.getUserId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);

        // 5. 返回登录成功，包含双令牌
               return LoginResp.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(refreshTokenExpiration)
                .build();
    }

    /** 注册 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterReq req) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getUsername());
        user.setEmail(req.getEmail());
        user.setStatus(1);
        userMapper.insert(user);

        // 默认角色：普通用户（USER，id=4，见 init.sql）
        userMapper.insertUserRole(user.getId(), 4L);
    }

    /** 刷新令牌 */
    @Override
    public LoginResp refresh(RefreshTokenReq req) {
        String refreshToken = req.getRefreshToken();
        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        if (!"refresh".equals(claims.get("type"))) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = Long.valueOf(claims.get("uid").toString());
        String jti = claims.getId();

        // GETDEL 原子取用：同一 RefreshToken 只能成功刷新一次（并发兜底，文档 7.1）
        String redisKey = redisRefreshKey(userId, jti);
        String stored = stringRedisTemplate.opsForValue().getAndDelete(redisKey);
        if (stored == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 轮换：签发新 AccessToken + 新 RefreshToken（旧的已 GETDEL 失效）
        String username = claims.getSubject();
        String newAccess = jwtTokenProvider.generateAccessToken(userId, username);
        String newRefresh = jwtTokenProvider.generateRefreshToken(userId, username);
        stringRedisTemplate.opsForValue().set(
                redisRefreshKey(userId, jwtTokenProvider.getJti(newRefresh)),
                username,
                Duration.ofMillis(refreshTokenExpiration));

        return LoginResp.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .expiresIn(refreshTokenExpiration)
                .build();
    }

    /** 退出登录 */
    @Override
    public void logout() {
        LoginUser current = SecurityUtils.getCurrentUser();
        // 清理该用户全部 RefreshToken（key 前缀匹配删除）
        var keys = stringRedisTemplate.keys(REFRESH_KEY_PREFIX + current.getUserId() + ":*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /** 忘记密码 */
    @Override
    public void forgotPassword(ForgotPasswordReq req) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getEmail, req.getEmail()));
        if (user == null) {
            // 不泄露邮箱是否存在：统一返回成功
            return;
        }

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                Constants.REDIS_KEY_RESET_TOKEN + resetToken,
                String.valueOf(user.getId()),
                Duration.ofMillis(Constants.RESET_TOKEN_TTL_MS));

        sendResetMail(user.getEmail(), resetToken);
        log.info("已向 {} 发送密码重置邮件", user.getEmail());
    }

    /** 发送密码重置邮件 */
    private void sendResetMail(String to, String token) {
        // 邮件 host 为占位（smtp.example.com）时无法真正发送，仅记录日志；
        // 配置真实 SMTP 后自动生效（application.yml spring.mail.*）
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("【CMS 内容管理系统】密码重置");
            message.setText("请点击以下链接重置密码（30 分钟内有效）：\n"
                    + "http://localhost:5173/reset-password?token=" + token);
            javaMailSender.send(message);
        } catch (Exception e) {
            log.warn("密码重置邮件发送失败（mail.host={}，请配置真实 SMTP）: {}", mailHost, e.getMessage());
        }
    }

    /** 重置密码 */
    @Override
    public void resetPassword(ResetPasswordReq req) {
        String userIdStr = stringRedisTemplate.opsForValue().get(
                Constants.REDIS_KEY_RESET_TOKEN + req.getToken());
        if (userIdStr == null) {
            throw new BusinessException(ResultCode.RESET_TOKEN_INVALID);
        }
        User user = userMapper.selectById(Long.valueOf(userIdStr));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(req.getPassword()));
        userMapper.updateById(update);
        // 一次性令牌：用后即删
        stringRedisTemplate.delete(Constants.REDIS_KEY_RESET_TOKEN + req.getToken());
    }

    /** 获取用户信息 */
    @Override
    public UserInfoResp getUserInfo() {
        LoginUser current = SecurityUtils.getCurrentUser();
        User user = userMapper.selectById(current.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionCodesByUserId(user.getId());
        return UserInfoResp.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    /** 更新用户信息 */
    @Override
    public void updateProfile(UpdateProfileReq req) {
        LoginUser current = SecurityUtils.getCurrentUser();
        User user = userMapper.selectById(current.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        User update = new User();
        update.setId(user.getId());
        if (StringUtils.hasText(req.getNickname())) {
            update.setNickname(req.getNickname().trim());
        }
        if (StringUtils.hasText(req.getEmail())) {
            update.setEmail(req.getEmail().trim());
        }
        if (StringUtils.hasText(req.getAvatar())) {
            update.setAvatar(req.getAvatar().trim());
        }
        userMapper.updateById(update);
    }

    /** 修改密码 */
    @Override
    public void changePassword(ChangePasswordReq req) {
        LoginUser current = SecurityUtils.getCurrentUser();
        User user = userMapper.selectById(current.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 校验原密码（BCrypt 比对），失败返回 1004
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_MISMATCH);
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(update);
        log.info("用户 {} 修改了密码", user.getUsername());
    }

    /** 分页查询用户 */
    @Override
    public Page<UserPageResp> pageUsers(long pageNum, long pageSize, String keyword, Integer status) {
        Page<User> page = new Page<>(pageNum, pageSize);
        var wrapper = Wrappers.<User>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(User::getUsername, keyword).or().like(User::getNickname, keyword))
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(page, wrapper);

        List<Long> userIds = result.getRecords().stream().map(User::getId).toList();
        Map<Long, List<String>> roleMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Map<String, Object> row : userMapper.selectRoleCodesByUserIds(userIds)) {
                Long uid = ((Number) row.get("userId")).longValue();
                roleMap.computeIfAbsent(uid, k -> new ArrayList<>()).add((String) row.get("code"));
            }
        }

        Page<UserPageResp> resp = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        resp.setRecords(result.getRecords().stream()
                .map(u -> toPageResp(u, roleMap.getOrDefault(u.getId(), List.of())))
                .toList());
        return resp;
    }

    /** 获取用户详情 */
    @Override
    public UserPageResp getUserDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toPageResp(user, userMapper.selectRoleCodesByUserId(id));
    }

    /** 创建用户 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserCreateReq req) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() == null ? req.getUsername() : req.getNickname());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        userMapper.insert(user);
        assignRoles(user.getId(), req.getRoleIds());
    }

    /** 更新用户信息 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UserUpdateReq req) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 保护：ADMIN 账号不可被禁用（文档 4.2 权限边界）
        if (req.getStatus() != null && req.getStatus() == 0
                && userMapper.selectRoleCodesByUserId(id).contains("ADMIN")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能禁用管理员账号");
        }
        User update = new User();
        update.setId(id);
        if (req.getNickname() != null) {
            update.setNickname(req.getNickname());
        }
        if (req.getEmail() != null) {
            update.setEmail(req.getEmail());
        }
        if (req.getPhone() != null) {
            update.setPhone(req.getPhone());
        }
        if (req.getStatus() != null) {
            update.setStatus(req.getStatus());
        }
        if (StringUtils.hasText(req.getPassword())) {
            update.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        // 至少一个业务字段才执行 UPDATE（避免空 UPDATE 报 SQL 语法错误，如仅改角色时）
        if (update.getNickname() != null || update.getEmail() != null
                || update.getPhone() != null || update.getStatus() != null || update.getPassword() != null) {
            userMapper.updateById(update);
        }
        if (req.getRoleIds() != null) {
            assignRoles(id, req.getRoleIds());
        }
    }

    /** 删除用户 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        if (id.equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能删除当前登录账号");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 保护：ADMIN 账号不可删除（文档 4.2 权限边界）
        if (userMapper.selectRoleCodesByUserId(id).contains("ADMIN")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能删除管理员账号");
        }
        userMapper.deleteUserRoles(id);
        userMapper.deleteById(id);
    }

    /** 重设用户角色：先清后插；为空则回退默认 USER 角色 */
    private void assignRoles(Long userId, List<Long> roleIds) {
        userMapper.deleteUserRoles(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            userMapper.insertUserRole(userId, 4L); // USER id=4（见 init.sql）
            return;
        }
        for (Long roleId : roleIds) {
            userMapper.insertUserRole(userId, roleId);
        }
    }

    /** 转换为分页响应实体 */
    private UserPageResp toPageResp(User user, List<String> roles) {
        return UserPageResp.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .createTime(user.getCreateTime())
                .roles(roles)
                .build();
    }

    /** 生成 刷新令牌缓存键名 */
    private String redisRefreshKey(Long userId, String jti) {
        return REFRESH_KEY_PREFIX + userId + ":" + jti;
    }
}
