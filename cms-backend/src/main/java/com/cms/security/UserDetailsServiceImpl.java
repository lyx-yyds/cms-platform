package com.cms.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cms.common.Constants;
import com.cms.common.ResultCode;
import com.cms.common.BusinessException;
import com.cms.entity.User;
import com.cms.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户认证服务（security/UserDetailsServiceImpl.java）
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // RBAC：user → role → permission（文档 7.3）
        // authorities = ROLE_xxx（角色编码）+ 权限点编码（sys_role_permission 关联，随角色权限编辑实时生效）
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissionCodes = userMapper.selectPermissionCodesByUserId(user.getId());
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        roleCodes.stream()
                .map(code -> new SimpleGrantedAuthority(Constants.roleCode(code)))
                .forEach(authorities::add);
        permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), authorities);
    }
}
