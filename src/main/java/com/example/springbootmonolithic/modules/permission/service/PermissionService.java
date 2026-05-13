package com.example.springbootmonolithic.modules.permission.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springbootmonolithic.modules.permission.dto.PermissionAssignRequest;
import com.example.springbootmonolithic.modules.permission.dto.PermissionCreateRequest;
import com.example.springbootmonolithic.modules.permission.dto.PermissionResponse;
import com.example.springbootmonolithic.modules.permission.dto.PermissionUpdateRequest;
import com.example.springbootmonolithic.modules.permission.entity.Permission;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService extends IService<Permission> {

    /**
     * 创建权限
     */
    PermissionResponse createPermission(PermissionCreateRequest request);

    /**
     * 更新权限
     */
    PermissionResponse updatePermission(Long id, PermissionUpdateRequest request);

    /**
     * 删除权限
     */
    void deletePermission(Long id);

    /**
     * 获取权限详情
     */
    PermissionResponse getPermissionDetail(Long id);

    /**
     * 获取所有权限列表
     */
    List<PermissionResponse> listPermissions();

    /**
     * 获取权限树
     */
    List<PermissionResponse> getPermissionTree();

    /**
     * 给角色分配权限
     */
    void assignPermissions(PermissionAssignRequest request);

    /**
     * 移除角色权限
     */
    void removePermissions(PermissionAssignRequest request);

    /**
     * 获取角色的权限编码列表
     */
    List<String> getPermissionCodesByRoleId(Long roleId);

    /**
     * 获取角色的权限列表
     */
    List<PermissionResponse> getPermissionsByRoleId(Long roleId);

    /**
     * 获取用户的权限编码列表（通过角色关联）
     */
    List<String> getPermissionCodesByUserId(Long userId);

    /**
     * 获取用户的权限列表（通过角色关联）
     */
    List<PermissionResponse> getPermissionsByUserId(Long userId);
}
