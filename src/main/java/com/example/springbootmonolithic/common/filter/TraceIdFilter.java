package com.example.springbootmonolithic.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 过滤器
 * <p>
 * 为每个请求生成/传播 TraceId，写入 MDC 和响应头，实现全链路追踪
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 优先从请求头获取（支持上游传播），否则生成新的 TraceId
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        // 写入 MDC（日志自动携带）
        MDC.put(MDC_TRACE_ID_KEY, traceId);

        // 写入请求属性（Controller中可获取）
        request.setAttribute(MDC_TRACE_ID_KEY, traceId);

        // 写入响应头（客户端可获取）
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理 MDC，防止线程池复用导致 TraceId 污染
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }

    /**
     * 生成 TraceId：32位不带横线的UUID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
