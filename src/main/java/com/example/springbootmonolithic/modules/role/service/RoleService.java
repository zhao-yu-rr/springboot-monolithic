package com.example.springbootmonolithic.modules.role.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springbootmonolithic.modules.role.dto.RoleAssignRequest;
import com.example.springbootmonolithic.modules.role.dto.RoleCreateRequest;
import com.example.springbootmonolithic.modules.role.dto.RoleResponse;
import com.example.springbootmonolithic.modules.role.dto.RoleUpdateRequest;
import com.example.springbootmonolithic.modules.role.entity.Role;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService extends IService<Role> {

    /**
     * 创建角色
     */
    RoleResponse createRole(RoleCreateRequest request);

    /**
     * 更新角色
     */
    RoleResponse updateRole(Long id, RoleUpdateRequest request);

    /**
     * 删除角色
     */
    void deleteRole(Long id);

    /**
     * 获取角色详情
     */
    RoleResponse getRoleDetail(Long id);

    /**
     * 获取所有角色列表
     */
    List<RoleResponse> listRoles();

    /**
     * 给用户分配角色
     */
    void assignRoles(RoleAssignRequest request);

    /**
     * 移除用户角色
     */
    void removeRoles(RoleAssignRequest request);

    /**
     * 获取用户的角色编码列表
     */
    List<String> getRoleCodesByUserId(Long userId);

    /**
     * 获取用户的角色列表
     */
    List<RoleResponse> getRolesByUserId(Long userId);

    /**
     * 获取角色的权限编码列表
     */
    List<String> getPermissionCodesByRoleId(Long roleId);
}
