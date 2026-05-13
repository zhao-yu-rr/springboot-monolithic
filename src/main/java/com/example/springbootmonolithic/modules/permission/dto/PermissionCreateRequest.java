package com.example.springbootmonolithic.modules.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建权限请求DTO
 */
@Data
public class PermissionCreateRequest {

    @NotBlank(message = "权限编码不能为空")
    @Size(min = 2, max = 50, message = "权限编码长度必须在2-50个字符之间")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "权限编码必须大写字母开头，只能包含大写字母、数字和下划线")
    private String permissionCode;

    @NotBlank(message = "权限名称不能为空")
    @Size(min = 2, max = 50, message = "权限名称长度必须在2-50个字符之间")
    private String permissionName;

    @NotBlank(message = "权限类型不能为空")
    @Pattern(regexp = "^(MENU|BUTTON|API)$", message = "权限类型必须是MENU、BUTTON或API")
    private String permissionType;

    private Long parentId;

    private Integer sortOrder;

    private String path;

    private String icon;

    @Size(max = 100, message = "描述长度不能超过100个字符")
    private String description;
}
