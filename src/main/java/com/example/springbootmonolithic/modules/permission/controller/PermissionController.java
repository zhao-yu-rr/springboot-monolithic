package com.example.springbootmonolithic.modules.permission.controller;

import com.example.springbootmonolithic.common.config.RequirePermission;
import com.example.springbootmonolithic.common.result.Result;
import com.example.springbootmonolithic.modules.permission.dto.PermissionAssignRequest;
import com.example.springbootmonolithic.modules.permission.dto.PermissionCreateRequest;
import com.example.springbootmonolithic.modules.permission.dto.PermissionResponse;
import com.example.springbootmonolithic.modules.permission.dto.PermissionUpdateRequest;
import com.example.springbootmonolithic.modules.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器（需Token认证）
 */
@Tag(name = "权限管理", description = "权限CRUD、权限分配")
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @RequirePermission("SYSTEM_PERMISSION_CREATE")
    @Operation(summary = "创建权限")
    @PostMapping
    public Result<PermissionResponse> createPermission(@Valid @RequestBody PermissionCreateRequest request) {
        PermissionResponse response = permissionService.createPermission(request);
        return Result.success(response);
    }

    @RequirePermission("SYSTEM_PERMISSION_UPDATE")
    @Operation(summary = "更新权限")
    @PutMapping("/{id}")
    public Result<PermissionResponse> updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionUpdateRequest request) {
        PermissionResponse response = permissionService.updatePermission(id, request);
        return Result.success(response);
    }

    @RequirePermission("SYSTEM_PERMISSION_DELETE")
    @Operation(summary = "删除权限")
    @DeleteMapping("/{id}")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success();
    }

    @RequirePermission("SYSTEM_PERMISSION_QUERY")
    @Operation(summary = "获取权限详情")
    @GetMapping("/{id}")
    public Result<PermissionResponse> getPermissionDetail(@PathVariable Long id) {
        PermissionResponse response = permissionService.getPermissionDetail(id);
        return Result.success(response);
    }

    @RequirePermission("SYSTEM_PERMISSION_QUERY")
    @Operation(summary = "获取权限列表")
    @GetMapping("/list")
    public Result<List<PermissionResponse>> listPermissions() {
        List<PermissionResponse> list = permissionService.listPermissions();
        return Result.success(list);
    }

    @RequirePermission("SYSTEM_PERMISSION_QUERY")
    @Operation(summary = "获取权限树")
    @GetMapping("/tree")
    public Result<List<PermissionResponse>> getPermissionTree() {
        List<PermissionResponse> tree = permissionService.getPermissionTree();
        return Result.success(tree);
    }

    @RequirePermission("SYSTEM_PERMISSION_ASSIGN")
    @Operation(summary = "给角色分配权限")
    @PostMapping("/assign")
    public Result<Void> assignPermissions(@Valid @RequestBody PermissionAssignRequest request) {
        permissionService.assignPermissions(request);
        return Result.success();
    }

    @RequirePermission("SYSTEM_PERMISSION_ASSIGN")
    @Operation(summary = "移除角色权限")
    @DeleteMapping("/assign")
    public Result<Void> removePermissions(@Valid @RequestBody PermissionAssignRequest request) {
        permissionService.removePermissions(request);
        return Result.success();
    }

    @RequirePermission("SYSTEM_PERMISSION_QUERY")
    @Operation(summary = "获取角色的权限列表")
    @GetMapping("/role/{roleId}")
    public Result<List<PermissionResponse>> getRolePermissions(@PathVariable Long roleId) {
        List<PermissionResponse> permissions = permissionService.getPermissionsByRoleId(roleId);
        return Result.success(permissions);
    }
}
