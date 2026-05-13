package com.example.springbootmonolithic.modules.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootmonolithic.modules.role.entity.Role;
import com.example.springbootmonolithic.modules.role.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户-角色关联Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 根据用户ID查询角色编码列表
     */
    List<String> selectRoleCodesByUserId(Long userId);

    /**
     * 根据用户ID查询角色ID列表
     */
    List<Long> selectRoleIdsByUserId(Long userId);

    /**
     * 根据用户ID查询角色列表
     */
    List<Role> selectRolesByUserId(Long userId);
}
