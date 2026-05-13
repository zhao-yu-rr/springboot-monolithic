package com.example.springbootmonolithic.modules.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springbootmonolithic.modules.user.dto.*;
import com.example.springbootmonolithic.modules.user.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    void register(RegisterRequest request);

    /**
     * 用户登录
     */
    CurrentUserResponse login(LoginRequest request);

    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);

    /**
     * 获取当前用户信息（含用户实体、角色编码、权限编码）
     */
    CurrentUserResponse getCurrentUser(Long userId);

    /**
     * 获取用户的角色编码列表
     */
    List<String> getRoleCodesByUserId(Long userId);

    /**
     * 分页查询用户列表
     */
    IPage<UserResponse> listUsers(int pageNum, int pageSize, String username, String nickname, Integer status);

    /**
     * 获取用户详情
     */
    UserResponse getUserDetail(Long id);

    /**
     * 管理员创建用户
     */
    UserResponse createUser(UserCreateRequest request);

    /**
     * 更新用户信息
     */
    UserResponse updateUser(Long id, UserUpdateRequest request);

    /**
     * 删除用户
     */
    void deleteUser(Long id);
}
