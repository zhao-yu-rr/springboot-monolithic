package com.example.springbootmonolithic.modules.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新角色请求DTO
 */
@Data
public class RoleUpdateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 30, message = "角色名称长度必须在2-30个字符之间")
    private String roleName;

    @Size(max = 100, message = "描述长度不能超过100个字符")
    private String description;

    private Integer status;
}
