package com.example.springbootmonolithic.modules.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootmonolithic.modules.permission.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 权限Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据用户ID查询权限编码列表
     */
    List<String> selectPermissionCodesByUserId(Long userId);

    /**
     * 根据角色ID查询权限编码列表
     */
    List<String> selectPermissionCodesByRoleId(Long roleId);

    /**
     * 根据角色ID查询权限列表
     */
    List<Permission> selectPermissionsByRoleId(Long roleId);

    /**
     * 根据用户ID查询权限列表（通过角色关联）
     */
    List<Permission> selectPermissionsByUserId(Long userId);
}
