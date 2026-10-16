package com.cms.security;

import com.cms.common.ResultCode;
import com.cms.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具（security/SecurityUtils.java）
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 当前登录用户（未登录抛 401） */
    public static LoginUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    /** 是否拥有指定角色编码 */
    public static boolean hasRole(String roleCode) {
        return getCurrentUser().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleCode));
    }
}
