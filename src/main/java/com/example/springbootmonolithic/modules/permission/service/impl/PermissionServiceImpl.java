package com.example.springbootmonolithic.modules.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springbootmonolithic.common.constant.ResultCode;
import com.example.springbootmonolithic.common.exception.BusinessException;
import com.example.springbootmonolithic.common.util.PermissionCacheUtil;
import com.example.springbootmonolithic.modules.permission.dto.PermissionAssignRequest;
import com.example.springbootmonolithic.modules.permission.dto.PermissionCreateRequest;
import com.example.springbootmonolithic.modules.permission.dto.PermissionResponse;
import com.example.springbootmonolithic.modules.permission.dto.PermissionUpdateRequest;
import com.example.springbootmonolithic.modules.permission.entity.Permission;
import com.example.springbootmonolithic.modules.permission.entity.RolePermission;
import com.example.springbootmonolithic.modules.permission.mapper.PermissionMapper;
import com.example.springbootmonolithic.modules.permission.mapper.RolePermissionMapper;
import com.example.springbootmonolithic.modules.permission.service.PermissionService;
import com.example.springbootmonolithic.modules.role.entity.Role;
import com.example.springbootmonolithic.modules.role.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final RolePermissionMapper rolePermissionMapper;
    private final RoleMapper roleMapper;
    private final PermissionCacheUtil permissionCacheUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionResponse createPermission(PermissionCreateRequest request) {
        // 检查权限编码是否已存在
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getPermissionCode, request.getPermissionCode());
        if (getOne(wrapper) != null) {
            throw new BusinessException(ResultCode.PERMISSION_ALREADY_EXISTS);
        }

        Permission permission = new Permission();
        permission.setPermissionCode(request.getPermissionCode());
        permission.setPermissionName(request.getPermissionName());
        permission.setPermissionType(request.getPermissionType());
        permission.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        permission.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        permission.setPath(request.getPath());
        permission.setIcon(request.getIcon());
        permission.setDescription(request.getDescription());
        permission.setStatus(1);
        save(permission);

        // 刷新全部权限编码缓存
        refreshAllPermissionCodesCache();

        log.info("权限创建成功: permissionCode={}", permission.getPermissionCode());
        return toResponse(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionResponse updatePermission(Long id, PermissionUpdateRequest request) {
        Permission permission = getById(id);
        if (permission == null) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }

        permission.setPermissionName(request.getPermissionName());
        permission.setParentId(request.getParentId() != null ? request.getParentId() : permission.getParentId());
        permission.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : permission.getSortOrder());
        permission.setPath(request.getPath());
        permission.setIcon(request.getIcon());
        permission.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            permission.setStatus(request.getStatus());
        }
        updateById(permission);

        // 如果状态变更，刷新全部权限编码缓存
        if (request.getStatus() != null) {
            refreshAllPermissionCodesCache();
        }

        log.info("权限更新成功: permissionId={}, permissionCode={}", id, permission.getPermissionCode());
        return toResponse(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long id) {
        Permission permission = getById(id);
        if (permission == null) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }

        // 检查是否有子权限
        LambdaQueryWrapper<Permission> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(Permission::getParentId, id);
        if (count(childWrapper) > 0) {
            throw new BusinessException(ResultCode.PERMISSION_IN_USE);
        }

        // 检查是否有角色关联此权限
        LambdaQueryWrapper<RolePermission> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.eq(RolePermission::getPermissionId, id);
        if (rolePermissionMapper.selectCount(rpWrapper) > 0) {
            throw new BusinessException(ResultCode.PERMISSION_IN_USE);
        }

        removeById(id);

        // 刷新全部权限编码缓存
        refreshAllPermissionCodesCache();

        log.info("权限删除成功: permissionId={}, permissionCode={}", id, permission.getPermissionCode());
    }

    @Override
    public PermissionResponse getPermissionDetail(Long id) {
        Permission permission = getById(id);
        if (permission == null) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }
        return toResponse(permission);
    }

    @Override
    public List<PermissionResponse> listPermissions() {
        List<Permission> permissions = list();
        return permissions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<PermissionResponse> getPermissionTree() {
        List<Permission> allPermissions = list();
        List<PermissionResponse> allResponses = allPermissions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // 构建树形结构
        Map<Long, PermissionResponse> map = new LinkedHashMap<>();
        for (PermissionResponse p : allResponses) {
            map.put(p.getId(), p);
        }

        List<PermissionResponse> tree = new ArrayList<>();
        for (PermissionResponse p : allResponses) {
            if (p.getParentId() == null || p.getParentId() == 0L) {
                tree.add(p);
            } else {
                PermissionResponse parent = map.get(p.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(p);
                }
            }
        }
        return tree;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(PermissionAssignRequest request) {
        // 检查角色是否存在
        Role role = roleMapper.selectById(request.getRoleId());
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }

        // 检查权限是否都存在
        List<Permission> permissions = listByIds(request.getPermissionIds());
        if (permissions.size() != request.getPermissionIds().size()) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }

        // 先删除角色现有权限
        LambdaQueryWrapper<RolePermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RolePermission::getRoleId, request.getRoleId());
        rolePermissionMapper.delete(deleteWrapper);

        // 重新分配权限
        for (Long permissionId : request.getPermissionIds()) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(request.getRoleId());
            rolePermission.setPermissionId(permissionId);
            rolePermissionMapper.insert(rolePermission);
        }

        log.info("权限分配成功: roleId={}, permissionIds={}", request.getRoleId(), request.getPermissionIds());

        // 刷新该角色的权限编码缓存
        refreshRolePermissionCodesCache(request.getRoleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePermissions(PermissionAssignRequest request) {
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, request.getRoleId())
                .in(RolePermission::getPermissionId, request.getPermissionIds());
        rolePermissionMapper.delete(wrapper);

        log.info("权限移除成功: roleId={}, permissionIds={}", request.getRoleId(), request.getPermissionIds());

        // 刷新该角色的权限编码缓存
        refreshRolePermissionCodesCache(request.getRoleId());
    }

    @Override
    public List<String> getPermissionCodesByRoleId(Long roleId) {
        return baseMapper.selectPermissionCodesByRoleId(roleId);
    }

    @Override
    public List<PermissionResponse> getPermissionsByRoleId(Long roleId) {
        List<Permission> permissions = baseMapper.selectPermissionsByRoleId(roleId);
        return permissions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        return baseMapper.selectPermissionCodesByUserId(userId);
    }

    @Override
    public List<PermissionResponse> getPermissionsByUserId(Long userId) {
        // 通过用户角色关联查询用户的所有权限
        List<Permission> permissions = baseMapper.selectPermissionsByUserId(userId);
        return permissions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .permissionCode(permission.getPermissionCode())
                .permissionName(permission.getPermissionName())
                .permissionType(permission.getPermissionType())
                .parentId(permission.getParentId())
                .sortOrder(permission.getSortOrder())
                .path(permission.getPath())
                .icon(permission.getIcon())
                .description(permission.getDescription())
                .status(permission.getStatus())
                .createTime(permission.getCreateTime())
                .updateTime(permission.getUpdateTime())
                .build();
    }

    // ========== 权限缓存刷新方法 ==========

    /**
     * 刷新全部权限编码缓存（perm:all:codes）
     */
    private void refreshAllPermissionCodesCache() {
        try {
            LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Permission::getStatus, 1);
            List<Permission> permissions = list(wrapper);
            Set<String> codes = permissions.stream()
                    .map(Permission::getPermissionCode)
                    .collect(Collectors.toSet());
            permissionCacheUtil.setAllPermissionCodes(codes);
            log.debug("刷新全部权限编码缓存: {} 条", codes.size());
        } catch (Exception e) {
            log.warn("刷新全部权限编码缓存失败", e);
        }
    }

    /**
     * 刷新指定角色的权限编码缓存（perm:role:codes:{roleId}）
     */
    private void refreshRolePermissionCodesCache(Long roleId) {
        try {
            List<String> codes = baseMapper.selectPermissionCodesByRoleId(roleId);
            permissionCacheUtil.setRolePermissionCodes(roleId, new HashSet<>(codes));
            log.debug("刷新角色[{}]权限编码缓存: {} 条", roleId, codes.size());
        } catch (Exception e) {
            log.warn("刷新角色[{}]权限编码缓存失败", roleId, e);
        }
    }
}
