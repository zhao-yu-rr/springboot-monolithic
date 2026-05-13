package com.example.springbootmonolithic.modules.permission.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限响应DTO
 */
@Data
@Builder
public class PermissionResponse {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private Long parentId;
    private Integer sortOrder;
    private String path;
    private String icon;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 子权限列表（树形结构）
     */
    @Builder.Default
    private List<PermissionResponse> children = null;
}
