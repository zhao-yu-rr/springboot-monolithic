package com.example.springbootmonolithic.modules.user.dto;

import com.example.springbootmonolithic.modules.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 当前用户扩展响应（包含用户信息、角色编码、权限编码）
 */
@Data
@Builder
public class CurrentUserResponse {

    /**
     * 用户基本信息
     */
    private User user;

    /**
     * 角色编码集合
     */
    private Set<String> roles;

    /**
     * 权限编码集合
     */
    private Set<String> permissions;

    /**
     * 登录token
     */
    private String token;
}
