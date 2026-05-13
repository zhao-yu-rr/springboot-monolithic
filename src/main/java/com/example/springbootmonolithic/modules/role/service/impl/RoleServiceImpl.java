package com.example.springbootmonolithic.modules.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springbootmonolithic.common.constant.ResultCode;
import com.example.springbootmonolithic.common.exception.BusinessException;
import com.example.springbootmonolithic.common.util.PermissionCacheUtil;
import com.example.springbootmonolithic.common.util.RoleCacheUtil;
import com.example.springbootmonolithic.modules.role.dto.RoleAssignRequest;
import com.example.springbootmonolithic.modules.role.dto.RoleCreateRequest;
import com.example.springbootmonolithic.modules.role.dto.RoleResponse;
import com.example.springbootmonolithic.modules.role.dto.RoleUpdateRequest;
import com.example.springbootmonolithic.modules.role.entity.Role;
import com.example.springbootmonolithic.modules.role.entity.UserRole;
import com.example.springbootmonolithic.modules.role.mapper.RoleMapper;
import com.example.springbootmonolithic.modules.role.mapper.UserRoleMapper;
import com.example.springbootmonolithic.modules.role.service.RoleService;
import com.example.springbootmonolithic.modules.permission.service.PermissionService;
import com.example.springbootmonolithic.modules.user.entity.User;
import com.example.springbootmonolithic.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final RoleCacheUtil roleCacheUtil;
    private final PermissionCacheUtil permissionCacheUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleResponse createRole(RoleCreateRequest request) {
        // 检查角色编码是否已存在
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getRoleCode, request.getRoleCode());
        if (getOne(wrapper) != null) {
            throw new BusinessException(ResultCode.ROLE_ALREADY_EXISTS);
        }

        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(1);
        save(role);

        // 刷新全部角色缓存
        refreshAllRoleCache();

        log.info("角色创建成功: roleCode={}", role.getRoleCode());
        return toResponse(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleResponse updateRole(Long id, RoleUpdateRequest request) {
        Role role = getById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }

        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        updateById(role);

        // 如果状态变更，刷新全部角色缓存和该角色的权限编码缓存
        if (request.getStatus() != null) {
            refreshAllRoleCache();
            refreshRolePermissionCodesCache(id);
        }

        log.info("角色更新成功: roleId={}, roleCode={}", id, role.getRoleCode());
        return toResponse(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }

        // 检查是否有用户关联此角色
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getRoleId, id);
        Long count = userRoleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ResultCode.ROLE_IN_USE);
        }

        removeById(id);

        // 刷新全部角色缓存，删除该角色的权限编码缓存
        refreshAllRoleCache();
        permissionCacheUtil.deleteRolePermissionCodes(id);

        log.info("角色删除成功: roleId={}, roleCode={}", id, role.getRoleCode());
    }

    @Override
    public RoleResponse getRoleDetail(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        return toResponse(role);
    }

    @Override
    public List<RoleResponse> listRoles() {
        List<Role> roles = list();
        return roles.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(RoleAssignRequest request) {
        // 检查用户是否存在
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查角色是否都存在
        List<Role> roles = listByIds(request.getRoleIds());
        if (roles.size() != request.getRoleIds().size()) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }

        // 先删除用户现有角色
        LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserRole::getUserId, request.getUserId());
        userRoleMapper.delete(deleteWrapper);

        // 重新分配角色
        for (Long roleId : request.getRoleIds()) {
            UserRole userRole = new UserRole();
            userRole.setUserId(request.getUserId());
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }

        log.info("角色分配成功: userId={}, roleIds={}", request.getUserId(), request.getRoleIds());

        // 刷新该用户的角色编码缓存
        refreshUserRoleCodesCache(request.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRoles(RoleAssignRequest request) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, request.getUserId())
                .in(UserRole::getRoleId, request.getRoleIds());
        userRoleMapper.delete(wrapper);

        log.info("角色移除成功: userId={}, roleIds={}", request.getUserId(), request.getRoleIds());

        // 刷新该用户的角色编码缓存
        refreshUserRoleCodesCache(request.getUserId());
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        return userRoleMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<RoleResponse> getRolesByUserId(Long userId) {
        List<Role> roles = userRoleMapper.selectRolesByUserId(userId);
        return roles.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionCodesByRoleId(Long roleId) {
        return permissionService.getPermissionCodesByRoleId(roleId);
    }

    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .status(role.getStatus())
                .createTime(role.getCreateTime())
                .updateTime(role.getUpdateTime())
                .permissionCodes(permissionService.getPermissionCodesByRoleId(role.getId()))
                .build();
    }

    // ========== 角色缓存刷新方法 ==========

    /**
     * 刷新全部角色缓存（role:all:map + role:all:codes）
     */
    private void refreshAllRoleCache() {
        try {
            List<Role> roles = list();
            // 刷新角色实体缓存
            roleCacheUtil.setAllRoles(roles);
            // 刷新角色编码缓存
            Set<String> codes = roles.stream()
                    .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                    .map(Role::getRoleCode)
                    .collect(Collectors.toSet());
            roleCacheUtil.setAllRoleCodes(codes);
            log.debug("刷新全部角色缓存: {} 个角色实体, {} 个启用角色编码", roles.size(), codes.size());
        } catch (Exception e) {
            log.warn("刷新全部角色缓存失败", e);
        }
    }

    /**
     * 刷新指定用户的角色编码缓存（role:user:codes:{userId}）
     */
    private void refreshUserRoleCodesCache(Long userId) {
        try {
            List<String> codes = userRoleMapper.selectRoleCodesByUserId(userId);
            roleCacheUtil.setUserRoleCodes(userId, new HashSet<>(codes));
            log.debug("刷新用户[{}]角色编码缓存: {} 条", userId, codes.size());
        } catch (Exception e) {
            log.warn("刷新用户[{}]角色编码缓存失败", userId, e);
        }
    }

    /**
     * 刷新指定角色的权限编码缓存（perm:role:codes:{roleId}）
     */
    private void refreshRolePermissionCodesCache(Long roleId) {
        try {
            List<String> codes = permissionService.getPermissionCodesByRoleId(roleId);
            permissionCacheUtil.setRolePermissionCodes(roleId, new HashSet<>(codes));
            log.debug("刷新角色[{}]权限编码缓存: {} 条", roleId, codes.size());
        } catch (Exception e) {
            log.warn("刷新角色[{}]权限编码缓存失败", roleId, e);
        }
    }
}
