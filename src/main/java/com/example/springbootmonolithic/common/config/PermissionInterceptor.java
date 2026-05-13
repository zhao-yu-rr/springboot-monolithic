package com.example.springbootmonolithic.common.config;

import com.example.springbootmonolithic.common.constant.ResultCode;
import com.example.springbootmonolithic.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 权限鉴权拦截器 - 在JWT认证之后、参数绑定之前执行
 * 检查@RequirePermission注解，校验用户是否拥有所需权限编码
 * 鉴权在@Valid参数校验之前，无权限直接返回403，不泄露参数错误信息
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 仅对Controller方法进行鉴权
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 获取方法上的@RequirePermission注解
        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            // 没有注解，放行
            return true;
        }

        // 从request属性中获取用户权限列表（由JWT拦截器设置）
        @SuppressWarnings("unchecked")
        List<String> userPermissions = (List<String>) request.getAttribute("permissions");

        String[] requiredPermissions = requirePermission.value();

        if (userPermissions == null || userPermissions.isEmpty()) {
            log.warn("用户无任何权限，拒绝访问: {} -> {}", request.getRequestURI(), handlerMethod.getMethod().getName());
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // OR逻辑：用户拥有任意一个所需权限即可通过
        boolean hasPermission = false;
        for (String required : requiredPermissions) {
            if (userPermissions.contains(required)) {
                hasPermission = true;
                break;
            }
        }

        if (!hasPermission) {
            log.warn("用户权限{}不满足要求{}，拒绝访问: {} -> {}",
                    userPermissions, List.of(requiredPermissions), request.getRequestURI(), handlerMethod.getMethod().getName());
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(),
                    "没有相关权限: 需要 " + List.of(requiredPermissions));
        }

        return true;
    }
}