package com.example.springbootmonolithic.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ========== 通用 ==========
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // ========== 客户端错误 4xx ==========
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),

    // ========== 用户错误 1xxx ==========
    USER_ALREADY_EXISTS(1001, "用户名已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USER_DISABLED(1004, "用户已被禁用"),
    TOKEN_INVALID(1005, "Token无效"),
    TOKEN_EXPIRED(1006, "Token已过期"),
    TOKEN_KICKED(1007, "账号已在其他设备登录，请重新登录"),

    // ========== 角色错误 2xxx ==========
    ROLE_ALREADY_EXISTS(2001, "角色编码已存在"),
    ROLE_NOT_FOUND(2002, "角色不存在"),
    ROLE_IN_USE(2003, "角色正在使用中，无法删除"),

    // ========== 权限错误 3xxx ==========
    PERMISSION_ALREADY_EXISTS(3001, "权限编码已存在"),
    PERMISSION_NOT_FOUND(3002, "权限不存在"),
    PERMISSION_IN_USE(3003, "权限正在使用中，无法删除"),

    // ========== 文件错误 4xxx ==========
    FILE_UPLOAD_FAILED(4001, "文件上传失败"),
    FILE_NOT_FOUND(4002, "文件不存在"),
    FILE_DOWNLOAD_FAILED(4003, "文件下载失败"),
    FILE_DELETE_FAILED(4004, "文件删除失败"),
    FILE_SIZE_EXCEEDED(4005, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(4006, "不支持的文件类型");

    private final int code;
    private final String message;
}
