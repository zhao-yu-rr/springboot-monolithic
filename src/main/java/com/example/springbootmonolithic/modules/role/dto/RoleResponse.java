package com.example.springbootmonolithic.modules.role.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色响应DTO
 */
@Data
@Builder
public class RoleResponse {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 角色拥有的权限编码列表
     */
    private List<String> permissionCodes;
}
