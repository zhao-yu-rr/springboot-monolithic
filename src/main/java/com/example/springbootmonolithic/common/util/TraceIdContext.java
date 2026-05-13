package com.example.springbootmonolithic.common.util;

import org.slf4j.MDC;

/**
 * TraceId 上下文工具类
 */
public class TraceIdContext {

    private TraceIdContext() {
    }

    /**
     * 获取当前请求的 TraceId
     */
    public static String getTraceId() {
        return MDC.get("traceId");
    }

    /**
     * 手动设置 TraceId（用于异步线程等场景）
     */
    public static void setTraceId(String traceId) {
        MDC.put("traceId", traceId);
    }

    /**
     * 清除 TraceId
     */
    public static void clear() {
        MDC.remove("traceId");
    }
}
