package com.example.springbootmonolithic.modules.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件上传/查询响应
 */
@Data
@Schema(description = "文件信息响应")
public class FileResponse {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String name;

    @Schema(description = "文件MIME类型")
    private String fileType;

    @Schema(description = "文件后缀")
    private String suffix;

    @Schema(description = "文件大小（字节）")
    private Long size;

    @Schema(description = "存储类型")
    private String storageType;

    @Schema(description = "存储Key")
    private String storageUrl;

    @Schema(description = "桶名")
    private String bucketName;

    @Schema(description = "预览地址")
    private String previewUrl;

    @Schema(description = "访问次数")
    private Integer visitCount;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
