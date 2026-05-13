package com.example.springbootmonolithic.modules.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootmonolithic.modules.file.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件管理 Mapper
 */
@Mapper
public interface FileMapper extends BaseMapper<SysFile> {
}
