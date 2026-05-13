package com.example.springbootmonolithic.common.config;

import com.example.springbootmonolithic.common.util.JwtUtil;
import com.example.springbootmonolithic.common.util.UserCacheUtil;
import com.example.springbootmonolithic.common.constant.ResultCode;
import com.example.springbootmonolithic.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC配置：CORS + JWT拦截器 + 权限拦截器 + TraceId
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final UserCacheUtil userCacheUtil;
    private final ObjectMapper objectMapper;
    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. JWT认证拦截器（设置userId和permissions到request属性）
        registry.addInterceptor(jwtAuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/error"
                );
        // 2. 权限鉴权拦截器（在JWT认证之后、@Valid参数校验之前执行）
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/error"
                );
    }

    @Bean
    public HandlerInterceptor jwtAuthInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                // 放行OPTIONS预检请求
                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    return true;
                }

                String token = jwtUtil.getTokenFromRequest(request);
                if (token == null || !jwtUtil.validateToken(token)) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED);
                    response.getWriter().write(objectMapper.writeValueAsString(result));
                    return false;
                }

                // 校验Token是否为当前用户最新有效的Token（单点登录：同一用户只能有一个有效Token）
                Long userId = jwtUtil.getUserIdFromToken(token);
                String cachedToken = userCacheUtil.getUserToken(userId);
                if (cachedToken == null || !cachedToken.equals(token)) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    Result<Void> result = Result.fail(ResultCode.TOKEN_KICKED);
                    response.getWriter().write(objectMapper.writeValueAsString(result));
                    return false;
                }

                // 将用户ID和权限列表存入请求属性，供Controller和权限拦截器使用
                List<String> permissions = jwtUtil.getPermissionsFromToken(token);
                request.setAttribute("userId", userId);
                request.setAttribute("permissions", permissions);
                return true;
            }
        };
    }

    /**
     * CORS跨域配置
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("Authorization");
        config.addExposedHeader("X-Trace-Id");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}