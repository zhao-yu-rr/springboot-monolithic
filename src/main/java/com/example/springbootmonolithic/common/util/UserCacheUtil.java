package com.example.springbootmonolithic.common.util;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户缓存工具类 - 封装用户相关的Redis业务操作
 */
@Component
@RequiredArgsConstructor
public class UserCacheUtil {

    private final RedissonClient redissonClient;

    private static final String USER_INFO_PREFIX = "user:info:";
    private static final String USER_ROLE_IDS_PREFIX = "user:role:ids:";
    private static final String USER_PERMISSION_LIST_PREFIX = "user:permissions:";
    private static final String USER_TOKEN_PREFIX = "user:token:";

    // ========== 用户信息缓存 ==========

    /**
     * 缓存用户信息
     */
    public void setUserInfo(Long userId, Object user, long timeout, TimeUnit unit) {
        redissonClient.getBucket(USER_INFO_PREFIX + userId).set(user, timeout, unit);
    }

    /**
     * 获取用户信息缓存
     */
    public <T> T getUserInfo(Long userId, Class<T> clazz) {
        RBucket<T> bucket = redissonClient.getBucket(USER_INFO_PREFIX + userId);
        return bucket.get();
    }

    /**
     * 删除用户信息缓存
     */
    public boolean deleteUserInfo(Long userId) {
        return redissonClient.getBucket(USER_INFO_PREFIX + userId).delete();
    }

    // ========== 用户角色ID缓存 ==========

    /**
     * 缓存用户角色ID列表
     */
    public void setUserRoleIds(Long userId, Set<Long> roleIds, long timeout, TimeUnit unit) {
        RSet<Long> set = redissonClient.getSet(USER_ROLE_IDS_PREFIX + userId);
        set.delete();
        set.addAll(roleIds);
        set.expire(timeout, unit);
    }

    /**
     * 获取用户角色ID列表缓存
     */
    public Set<Long> getUserRoleIds(Long userId) {
        RSet<Long> set = redissonClient.getSet(USER_ROLE_IDS_PREFIX + userId);
        return set.readAll();
    }

    /**
     * 删除用户角色ID列表缓存
     */
    public boolean deleteUserRoleIds(Long userId) {
        return redissonClient.getBucket(USER_ROLE_IDS_PREFIX + userId).delete();
    }

    // ========== 用户Token缓存 ==========

    /**
     * 缓存用户Token
     */
    public void setUserToken(Long userId, String token, long timeout, TimeUnit unit) {
        redissonClient.getBucket(USER_TOKEN_PREFIX + userId).set(token, timeout, unit);
    }

    /**
     * 获取用户Token
     */
    public String getUserToken(Long userId) {
        RBucket<String> bucket = redissonClient.getBucket(USER_TOKEN_PREFIX + userId);
        return bucket.get();
    }

    /**
     * 删除用户Token（踢人下线）
     */
    public boolean deleteUserToken(Long userId) {
        return redissonClient.getBucket(USER_TOKEN_PREFIX + userId).delete();
    }

    // ========== 用户权限编码缓存 ==========

    /**
     * 缓存用户权限编码集合
     */
    public void setUserPermissions(Long userId, Set<String> permissionCodes, long timeout, TimeUnit unit) {
        RSet<String> set = redissonClient.getSet(USER_PERMISSION_LIST_PREFIX + userId);
        set.delete();
        set.addAll(permissionCodes);
        set.expire(timeout, unit);
    }

    /**
     * 获取用户权限编码集合缓存
     */
    public Set<String> getUserPermissions(Long userId) {
        RSet<String> set = redissonClient.getSet(USER_PERMISSION_LIST_PREFIX + userId);
        return set.readAll();
    }

    /**
     * 删除用户权限编码集合缓存
     */
    public boolean deleteUserPermissions(Long userId) {
        return redissonClient.getBucket(USER_PERMISSION_LIST_PREFIX + userId).delete();
    }
}
