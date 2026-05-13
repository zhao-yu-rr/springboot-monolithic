package com.example.springbootmonolithic.common.util;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 权限缓存工具类 - 封装权限相关的Redis业务操作
 */
@Component
@RequiredArgsConstructor
public class PermissionCacheUtil {

    private final RedissonClient redissonClient;

    private static final String PERM_ALL_CODES_KEY = "perm:all:codes";
    private static final String PERM_ROLE_CODES_PREFIX = "perm:role:codes:";

    /**
     * 设置所有权限编码缓存
     */
    public void setAllPermissionCodes(Set<String> codes) {
        RSet<String> set = redissonClient.getSet(PERM_ALL_CODES_KEY);
        set.delete();
        set.addAll(codes);
    }

    /**
     * 获取所有权限编码缓存
     */
    public Set<String> getAllPermissionCodes() {
        RSet<String> set = redissonClient.getSet(PERM_ALL_CODES_KEY);
        return set.readAll();
    }

    /**
     * 判断权限编码是否存在
     */
    public boolean hasPermissionCode(String permissionCode) {
        RSet<String> set = redissonClient.getSet(PERM_ALL_CODES_KEY);
        return set.contains(permissionCode);
    }

    /**
     * 设置角色权限编码缓存
     */
    public void setRolePermissionCodes(Long roleId, Set<String> codes) {
        RSet<String> set = redissonClient.getSet(PERM_ROLE_CODES_PREFIX + roleId);
        set.delete();
        set.addAll(codes);
    }

    /**
     * 获取角色权限编码缓存
     */
    public Set<String> getRolePermissionCodes(Long roleId) {
        RSet<String> set = redissonClient.getSet(PERM_ROLE_CODES_PREFIX + roleId);
        return set.readAll();
    }

    /**
     * 删除角色权限编码缓存
     */
    public boolean deleteRolePermissionCodes(Long roleId) {
        return redissonClient.getBucket(PERM_ROLE_CODES_PREFIX + roleId).delete();
    }

    /**
     * 清空所有权限缓存
     */
    public void clearAllPermissionCache() {
        redissonClient.getBucket(PERM_ALL_CODES_KEY).delete();
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(PERM_ROLE_CODES_PREFIX + "*");
        for (String key : keys) {
            redissonClient.getBucket(key).delete();
        }
    }
}
