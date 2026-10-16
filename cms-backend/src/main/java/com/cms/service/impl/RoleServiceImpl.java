package com.cms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cms.common.BusinessException;
import com.cms.common.ResultCode;
import com.cms.dto.request.RoleReq;
import com.cms.dto.response.RoleResp;
import com.cms.entity.Role;
import com.cms.mapper.PermissionMapper;
import com.cms.mapper.RoleMapper;
import com.cms.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色权限管理实现（service/impl/RoleServiceImpl.java）
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public List<RoleResp> listRoles() {
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId)).stream()
                .map(r -> RoleResp.builder()
                        .id(r.getId())
                        .name(r.getName())
                        .code(r.getCode())
                        .description(r.getDescription())
                        .status(r.getStatus())
                        .permissions(permissionMapper.selectCodesByRoleId(r.getId()))
                        .build())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleReq req) {
        Long exists = roleMapper.selectCount(
                Wrappers.<Role>lambdaQuery().eq(Role::getCode, req.getCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色编码已存在");
        }
        Role role = new Role();
        role.setName(req.getName());
        role.setCode(req.getCode());
        role.setDescription(req.getDescription());
        role.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        roleMapper.insert(role);
        assignPermissions(role.getId(), req.getPermissionIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, RoleReq req) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Role update = new Role();
        update.setId(id);
        if (req.getName() != null) {
            update.setName(req.getName());
        }
        if (req.getCode() != null) {
            update.setCode(req.getCode());
        }
        if (req.getDescription() != null) {
            update.setDescription(req.getDescription());
        }
        if (req.getStatus() != null) {
            update.setStatus(req.getStatus());
        }
        roleMapper.updateById(update);
        if (req.getPermissionIds() != null) {
            assignPermissions(id, req.getPermissionIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        // 内置角色（init.sql id 1-4）禁止删除
        if (id <= 4) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "内置角色不允许删除");
        }
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long assigned = roleMapper.countAssignedUsers(id);
        if (assigned != null && assigned > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该角色已分配给用户，请先解除分配");
        }
        permissionMapper.deleteRolePermissions(id);
        roleMapper.deleteById(id);
    }

    /** 重设角色权限：先清后插 */
    private void assignPermissions(Long roleId, List<Long> permissionIds) {
        permissionMapper.deleteRolePermissions(roleId);
        if (permissionIds == null) {
            return;
        }
        for (Long pid : permissionIds) {
            permissionMapper.insertRolePermission(roleId, pid);
        }
    }
}
