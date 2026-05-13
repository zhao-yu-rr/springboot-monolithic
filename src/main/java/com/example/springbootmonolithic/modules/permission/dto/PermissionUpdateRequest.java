package com.example.springbootmonolithic.modules.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新权限请求DTO
 */
@Data
public class PermissionUpdateRequest {

    @NotBlank(message = "权限名称不能为空")
    @Size(min = 2, max = 50, message = "权限名称长度必须在2-50个字符之间")
    private String permissionName;

    private Long parentId;

    private Integer sortOrder;

    private String path;

    private String icon;

    @Size(max = 100, message = "描述长度不能超过100个字符")
    private String description;

    private Integer status;
}
