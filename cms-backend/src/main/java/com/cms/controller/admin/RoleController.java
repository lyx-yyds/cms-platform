package com.cms.controller.admin;

import com.cms.common.Result;
import com.cms.dto.request.RoleReq;
import com.cms.dto.response.RoleResp;
import com.cms.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色权限管理（controller/admin/RoleController.java，文档 7.3）
 */
@Tag(name = "管理端-角色权限")
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色列表（含权限，供用户分配角色）")
    @GetMapping
    public Result<List<RoleResp>> list() {
        return Result.ok(roleService.listRoles());
    }

    @Operation(summary = "创建角色（含权限关联）")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody RoleReq req) {
        roleService.createRole(req);
        return Result.ok();
    }

    @Operation(summary = "修改角色（重设权限）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleReq req) {
        roleService.updateRole(id, req);
        return Result.ok();
    }

    @Operation(summary = "删除角色（内置与已分配角色禁止删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.ok();
    }
}
