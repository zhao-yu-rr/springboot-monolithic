package com.example.springbootmonolithic.common.util;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis通用工具类 - 基于Redisson实现，仅提供通用Redis操作
 * 业务缓存操作请使用对应的 CacheUtil 类（UserCacheUtil / RoleCacheUtil / PermissionCacheUtil）
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedissonClient redissonClient;

    // ========== 通用操作 ==========

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(key).expire(timeout, unit);
    }

    /**
     * 获取过期时间（毫秒）
     */
    public long getExpire(String key, TimeUnit unit) {
        long remainMs = redissonClient.getBucket(key).remainTimeToLive();
        return unit.convert(remainMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 判断key是否存在
     */
    public boolean hasKey(String key) {
        return redissonClient.getKeys().countExists(key) > 0;
    }

    /**
     * 删除key
     */
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    /**
     * 批量删除key
     */
    public long delete(Collection<String> keys) {
        RKeys rKeys = redissonClient.getKeys();
        return rKeys.delete(keys.toArray(new String[0]));
    }

    // ========== String操作 ==========

    /**
     * 缓存值
     */
    public void set(String key, Object value) {
        redissonClient.getBucket(key).set(value);
    }

    /**
     * 缓存值（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redissonClient.getBucket(key).set(value, timeout, unit);
    }

    /**
     * 获取缓存值
     */
    public Object get(String key) {
        return redissonClient.getBucket(key).get();
    }

    /**
     * 获取缓存值（指定类型）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    // ========== Hash操作 ==========

    /**
     * HashPut
     */
    public void hPut(String key, String hashKey, Object value) {
        redissonClient.getMap(key).put(hashKey, value);
    }

    /**
     * HashGet
     */
    public Object hGet(String key, String hashKey) {
        RMap<Object, Object> map = redissonClient.getMap(key);
        return map.get(hashKey);
    }

    /**
     * HashDelete
     */
    public long hDelete(String key, Object... hashKeys) {
        RMap<Object, Object> map = redissonClient.getMap(key);
        long count = 0;
        for (Object hashKey : hashKeys) {
            if (map.remove(hashKey) != null) {
                count++;
            }
        }
        return count;
    }
}
