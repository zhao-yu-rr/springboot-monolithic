package com.example.springbootmonolithic.modules.user.controller;

import com.example.springbootmonolithic.common.result.Result;
import com.example.springbootmonolithic.modules.permission.service.PermissionService;
import com.example.springbootmonolithic.modules.role.service.RoleService;
import com.example.springbootmonolithic.modules.user.dto.CurrentUserResponse;
import com.example.springbootmonolithic.modules.user.dto.LoginRequest;
import com.example.springbootmonolithic.modules.user.dto.RegisterRequest;
import com.example.springbootmonolithic.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户注册、登录、获取信息")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<CurrentUserResponse> login(@Valid @RequestBody LoginRequest request) {
        CurrentUserResponse response = userService.login(request);
        return Result.success(response);
    }

    @Operation(summary = "获取当前用户信息（含角色和权限）")
    @GetMapping("/info")
    public Result<CurrentUserResponse> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        CurrentUserResponse currentUser = userService.getCurrentUser(userId);

        return Result.success(currentUser);
    }
}
