package com.cms.service;

import com.cms.dto.request.RoleReq;
import com.cms.dto.response.RoleResp;

import java.util.List;

/**
 * 角色权限管理服务（service/RoleService.java，文档 7.3 RBAC）
 */
public interface RoleService {

    /** 角色列表（含权限编码，供用户分配角色使用） */
    List<RoleResp> listRoles();

    /** 创建角色（含权限关联） */
    void createRole(RoleReq req);

    /** 修改角色（重设权限关联） */
    void updateRole(Long id, RoleReq req);

    /** 删除角色（内置角色与已分配角色禁止删除） */
    void deleteRole(Long id);
}
