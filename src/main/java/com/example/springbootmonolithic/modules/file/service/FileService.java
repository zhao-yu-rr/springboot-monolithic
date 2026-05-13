package com.example.springbootmonolithic.modules.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springbootmonolithic.modules.file.dto.FileResponse;
import com.example.springbootmonolithic.modules.file.entity.SysFile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件管理服务接口
 */
public interface FileService extends IService<SysFile> {

    /**
     * 上传文件到 RustFS
     *
     * @param file   上传的文件
     * @param userId 上传用户ID
     * @return 文件信息响应
     */
    FileResponse upload(MultipartFile file, Long userId);

    /**
     * 下载文件
     *
     * @param id 文件记录ID
     * @param response HTTP响应，用于写入文件流
     */
    void download(Long id, HttpServletResponse response);

    /**
     * 获取文件列表
     *
     * @param userId 用户ID（可选，用于过滤）
     * @return 文件列表
     */
    List<FileResponse> listFiles(Long userId);

    /**
     * 删除文件
     *
     * @param id     文件记录ID
     * @param userId 操作用户ID
     */
    void deleteFile(Long id, Long userId);
}
