package com.example.springbootmonolithic.common.util;

import com.example.springbootmonolithic.modules.role.entity.Role;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色缓存工具类 - 封装角色相关的Redis业务操作
 * <p>
 * 缓存结构：
 * 1. role:all:map - 所有角色实体数据（RMap<Long, Role>，key=roleId）
 * 2. role:all:codes - 所有启用状态的角色编码集合（由实体数据派生）
 * 3. role:user:codes:{userId} - 用户角色编码集合
 */
@Component
@RequiredArgsConstructor
public class RoleCacheUtil {

    private final RedissonClient redissonClient;

    private static final String ROLE_ALL_MAP_KEY = "role:all:map";
    private static final String ROLE_ALL_CODES_KEY = "role:all:codes";
    private static final String ROLE_USER_CODES_PREFIX = "role:user:codes:";

    // ========== 角色实体数据缓存（role:all:map） ==========

    /**
     * 批量设置所有角色实体缓存
     */
    public void setAllRoles(List<Role> roles) {
        RMap<Long, Role> map = redissonClient.getMap(ROLE_ALL_MAP_KEY);
        map.clear();
        Map<Long, Role> roleMap = roles.stream()
                .collect(Collectors.toMap(Role::getId, role -> role, (v1, v2) -> v1));
        map.putAll(roleMap);
    }

    /**
     * 获取所有角色实体缓存
     */
    public Map<Long, Role> getAllRoles() {
        RMap<Long, Role> map = redissonClient.getMap(ROLE_ALL_MAP_KEY);
        return map.readAllMap();
    }

    /**
     * 根据角色ID获取角色实体缓存
     */
    public Role getRoleById(Long roleId) {
        RMap<Long, Role> map = redissonClient.getMap(ROLE_ALL_MAP_KEY);
        return map.get(roleId);
    }

    // ========== 角色编码缓存（role:all:codes） ==========

    /**
     * 设置所有角色编码缓存
     */
    public void setAllRoleCodes(Set<String> codes) {
        RSet<String> set = redissonClient.getSet(ROLE_ALL_CODES_KEY);
        set.delete();
        set.addAll(codes);
    }

    /**
     * 获取所有角色编码缓存
     */
    public Set<String> getAllRoleCodes() {
        RSet<String> set = redissonClient.getSet(ROLE_ALL_CODES_KEY);
        return set.readAll();
    }

    /**
     * 判断角色编码是否存在
     */
    public boolean hasRoleCode(String roleCode) {
        RSet<String> set = redissonClient.getSet(ROLE_ALL_CODES_KEY);
        return set.contains(roleCode);
    }

    // ========== 用户角色编码缓存（role:user:codes:{userId}） ==========

    /**
     * 设置用户角色编码缓存
     */
    public void setUserRoleCodes(Long userId, Set<String> codes) {
        RSet<String> set = redissonClient.getSet(ROLE_USER_CODES_PREFIX + userId);
        set.delete();
        set.addAll(codes);
    }

    /**
     * 获取用户角色编码缓存
     */
    public Set<String> getUserRoleCodes(Long userId) {
        RSet<String> set = redissonClient.getSet(ROLE_USER_CODES_PREFIX + userId);
        return set.readAll();
    }

    /**
     * 删除用户角色编码缓存
     */
    public boolean deleteUserRoleCodes(Long userId) {
        return redissonClient.getBucket(ROLE_USER_CODES_PREFIX + userId).delete();
    }

    // ========== 缓存清空 ==========

    /**
     * 清空所有角色缓存
     */
    public void clearAllRoleCache() {
        redissonClient.getMap(ROLE_ALL_MAP_KEY).delete();
        redissonClient.getBucket(ROLE_ALL_CODES_KEY).delete();
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(ROLE_USER_CODES_PREFIX + "*");
        for (String key : keys) {
            redissonClient.getBucket(key).delete();
        }
    }
}
