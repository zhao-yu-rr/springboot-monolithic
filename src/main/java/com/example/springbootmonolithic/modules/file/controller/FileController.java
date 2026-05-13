package com.example.springbootmonolithic.modules.file.controller;

import com.example.springbootmonolithic.common.config.RequirePermission;
import com.example.springbootmonolithic.common.result.Result;
import com.example.springbootmonolithic.modules.file.dto.FileResponse;
import com.example.springbootmonolithic.modules.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件管理控制器
 */
@Tag(name = "文件管理", description = "文件上传、下载、列表、删除")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @RequirePermission("SYSTEM_FILE_UPLOAD")
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<FileResponse> upload(
            @Parameter(description = "上传的文件") @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        FileResponse response = fileService.upload(file, userId);
        return Result.success(response);
    }

    @RequirePermission("SYSTEM_FILE_DOWNLOAD")
    @Operation(summary = "下载文件")
    @GetMapping("/download/{id}")
    public void download(
            @Parameter(description = "文件ID") @PathVariable Long id,
            HttpServletResponse response) {
        fileService.download(id, response);
    }

    @RequirePermission("SYSTEM_FILE_DOWNLOAD")
    @Operation(summary = "获取文件列表")
    @GetMapping("/list")
    public Result<List<FileResponse>> listFiles(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<FileResponse> files = fileService.listFiles(userId);
        return Result.success(files);
    }

    @RequirePermission("SYSTEM_FILE_DELETE")
    @Operation(summary = "删除文件")
    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(
            @Parameter(description = "文件ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        fileService.deleteFile(id, userId);
        return Result.success();
    }
}
