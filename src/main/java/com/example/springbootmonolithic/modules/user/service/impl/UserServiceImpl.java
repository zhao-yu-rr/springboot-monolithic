package com.example.springbootmonolithic.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springbootmonolithic.common.constant.ResultCode;
import com.example.springbootmonolithic.common.exception.BusinessException;
import com.example.springbootmonolithic.common.util.JwtUtil;
import com.example.springbootmonolithic.common.util.RoleCacheUtil;
import com.example.springbootmonolithic.common.util.UserCacheUtil;
import com.example.springbootmonolithic.modules.permission.mapper.PermissionMapper;
import com.example.springbootmonolithic.modules.role.entity.Role;
import com.example.springbootmonolithic.modules.role.entity.UserRole;
import com.example.springbootmonolithic.modules.role.mapper.RoleMapper;
import com.example.springbootmonolithic.modules.role.mapper.UserRoleMapper;
import com.example.springbootmonolithic.modules.role.service.RoleService;
import com.example.springbootmonolithic.modules.user.dto.*;
import com.example.springbootmonolithic.modules.user.entity.User;
import com.example.springbootmonolithic.modules.user.mapper.UserMapper;
import com.example.springbootmonolithic.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;
    private final UserCacheUtil userCacheUtil;
    private final RoleCacheUtil roleCacheUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RoleService roleService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${user.register.default-role-code}")
    private String defaultRoleCode;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        // 检查用户名是否已存在
        User existingUser = getByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(1);

        save(user);

        // 分配默认角色
        assignDefaultRole(user.getId());

        log.info("用户注册成功: username={}, defaultRole={}", user.getUsername(), defaultRoleCode);
    }

    /**
     * 为用户分配默认角色
     */
    private void assignDefaultRole(Long userId) {
        LambdaQueryWrapper<Role> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(Role::getRoleCode, defaultRoleCode);
        Role role = roleMapper.selectOne(roleWrapper);
        if (role == null) {
            log.warn("默认角色不存在: roleCode={}", defaultRoleCode);
            return;
        }

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
        log.info("分配默认角色: userId={}, roleId={}, roleCode={}", userId, role.getId(), defaultRoleCode);
    }

    @Override
    public CurrentUserResponse login(LoginRequest request) {
        // 查找用户
        User user = getByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 检查状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 获取用户角色编码列表
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(user.getId());
        if (roleCodes == null || roleCodes.isEmpty()) {
            roleCodes = Collections.emptyList();
        }

        // 缓存用户角色编码到Redis（role:user:codes:{userId}）
        if (!roleCodes.isEmpty()) {
            roleCacheUtil.setUserRoleCodes(user.getId(), new HashSet<>(roleCodes));
        }

        // 获取用户权限编码列表（通过角色关联）
        List<String> permissionCodes = permissionMapper.selectPermissionCodesByUserId(user.getId());
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            permissionCodes = Collections.emptyList();
        }

        // 生成Token（含角色和权限信息）
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCodes, permissionCodes);

        // 缓存Token到Redis
        long expirationHours = jwtExpiration / (1000 * 60 * 60);
        userCacheUtil.setUserToken(user.getId(), token, expirationHours, TimeUnit.HOURS);

        // 缓存用户信息到Redis
        userCacheUtil.setUserInfo(user.getId(), user, expirationHours, TimeUnit.HOURS);

        // 缓存用户角色ID列表到Redis
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        if (roleIds != null && !roleIds.isEmpty()) {
            userCacheUtil.setUserRoleIds(user.getId(), new HashSet<>(roleIds), expirationHours, TimeUnit.HOURS);
        }

        // 缓存用户权限编码到Redis
        if (!permissionCodes.isEmpty()) {
            userCacheUtil.setUserPermissions(user.getId(), new HashSet<>(permissionCodes), expirationHours, TimeUnit.HOURS);
        }

        log.info("用户登录成功: username={}, roles={}, permissions={}", user.getUsername(), roleCodes, permissionCodes);

        return CurrentUserResponse.builder()
                .user(user)
                .roles(new HashSet<>(roleCodes))
                .permissions(new HashSet<>(permissionCodes))
                .token(token)
                .build();
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return getOne(wrapper);
    }

    @Override
    public CurrentUserResponse getCurrentUser(Long userId) {
        // 优先从缓存获取用户信息
        User cachedUser = userCacheUtil.getUserInfo(userId, User.class);
        if (cachedUser == null) {
            // 缓存未命中，查询数据库
            User user = getById(userId);
            if (user == null) {
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 回写缓存
            long expirationHours = jwtExpiration / (1000 * 60 * 60);
            userCacheUtil.setUserInfo(userId, user, expirationHours, TimeUnit.HOURS);

            cachedUser = user;
        }

        /**
         * 获取用户角色编码缓存
         */
        Set<String> roles = roleCacheUtil.getUserRoleCodes(userId);

        if (roles == null || roles.isEmpty()) {
            // 获取用户角色编码列表
            List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(userId);
            if (roleCodes == null || roleCodes.isEmpty()) {
                roleCodes = Collections.emptyList();
            }

            // 缓存用户角色编码到Redis（role:user:codes:{userId}）
            if (!roleCodes.isEmpty()) {
                roleCacheUtil.setUserRoleCodes(userId, new HashSet<>(roleCodes));
            }

            roles = new HashSet<>(roleCodes);
        }

        /**
         * 获取用户权限编码集合缓存
         */
        Set<String> permissions = userCacheUtil.getUserPermissions(userId);

        if (permissions == null || permissions.isEmpty()) {
            // 获取用户权限编码列表（通过角色关联）
            List<String> permissionCodes = permissionMapper.selectPermissionCodesByUserId(userId);
            if (permissionCodes == null || permissionCodes.isEmpty()) {
                permissionCodes = Collections.emptyList();
            }

            // 缓存用户权限编码到Redis
            if (!permissionCodes.isEmpty()) {
                long expirationHours = jwtExpiration / (1000 * 60 * 60);
                userCacheUtil.setUserPermissions(userId, new HashSet<>(permissionCodes), expirationHours, TimeUnit.HOURS);
            }

            permissions = new HashSet<>(permissionCodes);
        }

        return CurrentUserResponse.builder()
                .user(cachedUser)
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(userId);
        return roleCodes != null ? roleCodes : Collections.emptyList();
    }

    @Override
    public IPage<UserResponse> listUsers(int pageNum, int pageSize, String username, String nickname, Integer status) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), User::getUsername, username)
                .like(StringUtils.hasText(nickname), User::getNickname, nickname)
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getCreateTime);

        IPage<User> userPage = page(page, wrapper);

        // 转换为UserResponse，填充角色信息
        IPage<UserResponse> responsePage = userPage.convert(this::toResponse);
        return responsePage;
    }

    @Override
    public UserResponse getUserDetail(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(UserCreateRequest request) {
        // 检查用户名是否已存在
        User existingUser = getByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(1);

        save(user);

        // 分配角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            // 校验角色是否存在
            List<Role> roles = roleMapper.selectBatchIds(request.getRoleIds());
            if (roles.size() != request.getRoleIds().size()) {
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
            }
            for (Long roleId : request.getRoleIds()) {
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        } else {
            // 分配默认角色
            assignDefaultRole(user.getId());
        }

        log.info("管理员创建用户成功: username={}, roleIds={}", user.getUsername(), request.getRoleIds());
        return toResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        updateById(user);

        // 刷新用户缓存
        refreshUserCache(id);

        log.info("用户更新成功: userId={}", id);
        return toResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 逻辑删除
        removeById(id);

        // 删除用户-角色关联
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, id);
        userRoleMapper.delete(wrapper);

        // 清除用户缓存
        clearUserCache(id);

        log.info("用户删除成功: userId={}, username={}", id, user.getUsername());
    }

    // ========== 私有方法 ==========

    /**
     * User实体转UserResponse，填充角色编码
     */
    private UserResponse toResponse(User user) {
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(user.getId());
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .roleCodes(roleCodes != null ? roleCodes : Collections.emptyList())
                .build();
    }

    /**
     * 刷新用户缓存
     */
    private void refreshUserCache(Long userId) {
        try {
            long expirationHours = jwtExpiration / (1000 * 60 * 60);
            User user = getById(userId);
            if (user != null) {
                userCacheUtil.setUserInfo(userId, user, expirationHours, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.warn("刷新用户[{}]缓存失败", userId, e);
        }
    }

    /**
     * 清除用户缓存
     */
    private void clearUserCache(Long userId) {
        try {
            userCacheUtil.deleteUserInfo(userId);
            userCacheUtil.deleteUserToken(userId);
            userCacheUtil.deleteUserRoleIds(userId);
            userCacheUtil.deleteUserPermissions(userId);
            roleCacheUtil.deleteUserRoleCodes(userId);
        } catch (Exception e) {
            log.warn("清除用户[{}]缓存失败", userId, e);
        }
    }
}
