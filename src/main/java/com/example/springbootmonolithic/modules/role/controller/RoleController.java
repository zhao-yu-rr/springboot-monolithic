package com.example.springbootmonolithic.modules.role.controller;

import com.example.springbootmonolithic.common.config.RequirePermission;
import com.example.springbootmonolithic.common.result.Result;
import com.example.springbootmonolithic.modules.permission.dto.PermissionResponse;
import com.example.springbootmonolithic.modules.permission.service.PermissionService;
import com.example.springbootmonolithic.modules.role.dto.RoleAssignRequest;
import com.example.springbootmonolithic.modules.role.dto.RoleCreateRequest;
import com.example.springbootmonolithic.modules.role.dto.RoleResponse;
import com.example.springbootmonolithic.modules.role.dto.RoleUpdateRequest;
import com.example.springbootmonolithic.modules.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器（需Token认证）
 */
@Tag(name = "角色管理", description = "角色CRUD、角色分配")
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @RequirePermission("SYSTEM_ROLE_CREATE")
    @Operation(summary = "创建角色")
    @PostMapping
    public Result<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        RoleResponse response = roleService.createRole(request);
        return Result.success(response);
    }

    @RequirePermission("SYSTEM_ROLE_UPDATE")
    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public Result<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        RoleResponse response = roleService.updateRole(id, request);
        return Result.success(response);
    }

    @RequirePermission("SYSTEM_ROLE_DELETE")
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @RequirePermission("SYSTEM_ROLE_QUERY")
    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<RoleResponse> getRoleDetail(@PathVariable Long id) {
        RoleResponse response = roleService.getRoleDetail(id);
        return Result.success(response);
    }

    @RequirePermission("SYSTEM_ROLE_QUERY")
    @Operation(summary = "获取角色列表")
    @GetMapping("/list")
    public Result<List<RoleResponse>> listRoles() {
        List<RoleResponse> list = roleService.listRoles();
        return Result.success(list);
    }

    @RequirePermission("SYSTEM_ROLE_ASSIGN")
    @Operation(summary = "给用户分配角色")
    @PostMapping("/assign")
    public Result<Void> assignRoles(@Valid @RequestBody RoleAssignRequest request) {
        roleService.assignRoles(request);
        return Result.success();
    }

    @RequirePermission("SYSTEM_ROLE_ASSIGN")
    @Operation(summary = "移除用户角色")
    @DeleteMapping("/assign")
    public Result<Void> removeRoles(@Valid @RequestBody RoleAssignRequest request) {
        roleService.removeRoles(request);
        return Result.success();
    }

    @RequirePermission("SYSTEM_ROLE_QUERY")
    @Operation(summary = "获取用户的角色列表")
    @GetMapping("/user/{userId}")
    public Result<List<RoleResponse>> getUserRoles(@PathVariable Long userId) {
        List<RoleResponse> roles = roleService.getRolesByUserId(userId);
        return Result.success(roles);
    }

    @RequirePermission("SYSTEM_ROLE_QUERY")
    @Operation(summary = "获取角色的权限列表")
    @GetMapping("/{roleId}/permissions")
    public Result<List<PermissionResponse>> getRolePermissions(@PathVariable Long roleId) {
        List<PermissionResponse> permissions = permissionService.getPermissionsByRoleId(roleId);
        return Result.success(permissions);
    }
}
