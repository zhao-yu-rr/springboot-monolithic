package com.example.springbootmonolithic.modules.role.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色分配请求DTO（给用户分配角色）
 */
@Data
public class RoleAssignRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotEmpty(message = "角色列表不能为空")
    private List<Long> roleIds;
}
