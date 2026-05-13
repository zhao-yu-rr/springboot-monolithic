package com.example.springbootmonolithic.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springbootmonolithic.common.util.PermissionCacheUtil;
import com.example.springbootmonolithic.common.util.RoleCacheUtil;
import com.example.springbootmonolithic.modules.permission.entity.Permission;
import com.example.springbootmonolithic.modules.permission.mapper.PermissionMapper;
import com.example.springbootmonolithic.modules.role.entity.Role;
import com.example.springbootmonolithic.modules.role.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC缓存初始化器 - 项目启动时将角色和权限数据加载到Redis缓存
 * <p>
 * 缓存结构：
 * 1. role:all:map - 所有角色实体数据（RMap<Long, Role>，key=roleId）
 * 2. role:all:codes - 所有启用状态的角色编码集合（由实体数据派生）
 * 3. perm:all:codes - 所有启用状态的权限编码集合
 * 4. perm:role:codes:{roleId} - 每个角色的权限编码集合
 * <p>
 * 不缓存用户相关数据（用户-角色关联），用户权限在登录时实时查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RBACCacheInitializer implements CommandLineRunner {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RoleCacheUtil roleCacheUtil;
    private final PermissionCacheUtil permissionCacheUtil;

    @Override
    public void run(String... args) {
        log.info("======== 开始初始化RBAC缓存 ========");
        try {
            // 1. 初始化角色缓存
            loadAllRoles();

            // 2. 初始化权限缓存
            loadAllPermissionCodes();
            loadRolePermissionCodes();

            log.info("======== RBAC缓存初始化完成 ========");
        } catch (Exception e) {
            log.error("RBAC缓存初始化失败，项目仍可正常运行（将回退到数据库查询）", e);
        }
    }

    // ========== 角色缓存初始化 ==========

    /**
     * 加载所有角色表数据到Redis缓存
     */
    private void loadAllRoles() {
        List<Role> roles = roleMapper.selectList(null);
        if (roles.isEmpty()) {
            log.warn("角色表无数据，跳过角色缓存初始化");
            return;
        }

        // 缓存完整角色实体数据
        roleCacheUtil.setAllRoles(roles);

        // 同时派生缓存角色编码集合，便于快速校验
        Set<String> codes = roles.stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .map(Role::getRoleCode)
                .collect(Collectors.toSet());
        roleCacheUtil.setAllRoleCodes(codes);

        log.info("加载角色表数据缓存: {} 个角色实体, {} 个启用角色编码", roles.size(), codes.size());
    }

    // ========== 权限缓存初始化 ==========

    /**
     * 加载所有启用状态的权限编码到Redis
     */
    private void loadAllPermissionCodes() {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getStatus, 1);
        List<Permission> permissions = permissionMapper.selectList(wrapper);

        Set<String> codes = permissions.stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toSet());

        permissionCacheUtil.setAllPermissionCodes(codes);
        log.info("加载全部权限编码缓存: {} 条", codes.size());
    }

    /**
     * 加载每个角色的权限编码到Redis
     */
    private void loadRolePermissionCodes() {
        List<Role> roles = roleMapper.selectList(null);
        int totalRoleCodes = 0;

        for (Role role : roles) {
            List<String> codes = permissionMapper.selectPermissionCodesByRoleId(role.getId());
            Set<String> codeSet = new HashSet<>(codes);
            permissionCacheUtil.setRolePermissionCodes(role.getId(), codeSet);
            totalRoleCodes += codeSet.size();
            log.debug("角色[{}]权限编码缓存: {} 条", role.getRoleCode(), codeSet.size());
        }

        log.info("加载角色权限编码缓存: {} 个角色, 共 {} 条权限编码", roles.size(), totalRoleCodes);
    }
}
