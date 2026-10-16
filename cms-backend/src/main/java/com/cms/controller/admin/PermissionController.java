package com.cms.controller.admin;

import com.cms.common.Result;
import com.cms.entity.Permission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cms.mapper.PermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限列表（controller/admin/PermissionController.java，供角色管理页勾选权限）
 * 接口总览未单独列出，属角色权限管理（文档 7.3 / 第二阶段）配套接口
 */
@Tag(name = "管理端-权限")
@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class PermissionController {

    private final PermissionMapper permissionMapper;

    @Operation(summary = "权限列表（全量，供角色分配权限）")
    @GetMapping
    public Result<List<Permission>> list() {
        return Result.ok(permissionMapper.selectList(
                Wrappers.<Permission>lambdaQuery().orderByAsc(Permission::getSortOrder).orderByAsc(Permission::getId)));
    }
}
