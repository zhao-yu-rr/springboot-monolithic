package com.example.springbootmonolithic.modules.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootmonolithic.modules.permission.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-权限关联Mapper
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
