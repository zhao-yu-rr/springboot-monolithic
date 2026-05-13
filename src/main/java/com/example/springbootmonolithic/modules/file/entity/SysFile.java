package com.example.springbootmonolithic.modules.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件管理实体
 */
@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名 */
    private String name;

    /** 原始文件名（备用） */
    private String original;

    /** 文件MIME类型 */
    private String fileType;

    /** 文件后缀 */
    private String suffix;

    /** 文件大小（字节） */
    private Long size;

    /** 存储类型 */
    private String storageType;

    /** 存储地址（对象Key） */
    private String storageUrl;

    /** 桶名 */
    private String bucketName;

    /** 桶内文件名 */
    private String objectName;

    /** 预览地址 */
    private String previewUrl;

    /** 访问次数 */
    private Integer visitCount;

    /** 创建人ID */
    private Long createBy;

    /** 修改人ID */
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            return super.toString();
        }
    }
}
