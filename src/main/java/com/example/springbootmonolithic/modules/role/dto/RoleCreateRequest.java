package com.example.springbootmonolithic.modules.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建角色请求DTO
 */
@Data
public class RoleCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    @Size(min = 2, max = 30, message = "角色编码长度必须在2-30个字符之间")
    @jakarta.validation.constraints.Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "角色编码必须大写字母开头，只能包含大写字母、数字和下划线")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 30, message = "角色名称长度必须在2-30个字符之间")
    private String roleName;

    @Size(max = 100, message = "描述长度不能超过100个字符")
    private String description;
}
