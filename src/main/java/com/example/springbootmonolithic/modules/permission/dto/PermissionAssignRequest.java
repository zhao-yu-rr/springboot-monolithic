package com.example.springbootmonolithic.modules.permission.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 权限分配请求DTO（给角色分配权限）
 */
@Data
public class PermissionAssignRequest {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @NotEmpty(message = "权限列表不能为空")
    private List<Long> permissionIds;
}
