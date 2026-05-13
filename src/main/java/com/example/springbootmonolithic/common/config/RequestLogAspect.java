package com.example.springbootmonolithic.common.config;

import com.example.springbootmonolithic.common.util.TraceIdContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * AOP请求日志
 */
@Slf4j
@Aspect
@Component
public class RequestLogAspect {

    @Pointcut("execution(* com.example.springbootmonolithic.modules..controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String traceId = TraceIdContext.getTraceId();
        Object[] args = joinPoint.getArgs();

        log.info(">>> [{}] {} {} | {}.{} | args={}", traceId, method, uri, className, methodName,
                Arrays.stream(args)
                        .filter(arg -> !(arg instanceof HttpServletRequest))
                        .filter(arg -> !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                        .toList());

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - startTime;

        log.info("<<< [{}] {} {} | {}.{} | {}ms", traceId, method, uri, className, methodName, elapsed);
        return result;
    }
}
