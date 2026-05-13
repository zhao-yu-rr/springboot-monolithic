package com.example.springbootmonolithic.modules.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.springbootmonolithic.common.config.RequirePermission;
import com.example.springbootmonolithic.common.result.Result;
import com.example.springbootmonolithic.modules.user.dto.UserCreateRequest;
import com.example.springbootmonolithic.modules.user.dto.UserResponse;
import com.example.springbootmonolithic.modules.user.dto.UserUpdateRequest;
import com.example.springbootmonolithic.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器（需Token认证 + 权限鉴权）
 */
@Tag(name = "用户管理", description = "用户CRUD、用户列表")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @RequirePermission("SYSTEM_USER_QUERY")
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/list")
    public Result<IPage<UserResponse>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer status) {
        IPage<UserResponse> page = userService.listUsers(pageNum, pageSize, username, nickname, status);
        return Result.success(page);
    }

    @RequirePermission("SYSTEM_USER_QUERY")
    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<UserResponse> getUserDetail(@PathVariable Long id) {
        UserResponse response = userService.getUserDetail(id);
        return Result.success(response);
    }

    @RequirePermission("SYSTEM_USER_CREATE")
    @Operation(summary = "创建用户")
    @PostMapping
    public Result<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return Result.success(response);
    }

    @RequirePermission("SYSTEM_USER_UPDATE")
    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    public Result<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return Result.success(response);
    }

    @RequirePermission("SYSTEM_USER_DELETE")
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
