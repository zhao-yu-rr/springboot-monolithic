package com.example.springbootmonolithic.modules.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springbootmonolithic.common.constant.ResultCode;
import com.example.springbootmonolithic.common.exception.BusinessException;
import com.example.springbootmonolithic.modules.file.dto.FileResponse;
import com.example.springbootmonolithic.modules.file.entity.SysFile;
import com.example.springbootmonolithic.modules.file.mapper.FileMapper;
import com.example.springbootmonolithic.modules.file.service.FileService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileMapper, SysFile> implements FileService {

    private final S3Client s3Client;

    @Value("${rustfs.bucket-name}")
    private String bucketName;

    @Value("${rustfs.preview-domain:http://localhost:9000}")
    private String previewDomain;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileResponse upload(MultipartFile file, Long userId) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件名不能为空");
        }

        // 生成唯一文件名：日期/用户ID/UUID.后缀
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String suffix = getSuffix(originalFilename);
        String objectName = String.format("%s/%d/%s.%s", datePath, userId, UUID.randomUUID().toString().replace("-", ""), suffix);

        // 确保存储桶存在
        ensureBucketExists();

        try {
            // 上传到 RustFS (S3)
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .contentType(file.getContentType())
                    .contentDisposition("inline")
                    .build();

            s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

            // 保存文件记录到数据库
            SysFile sysFile = new SysFile();
            sysFile.setName(originalFilename);
            sysFile.setOriginal(originalFilename);
            sysFile.setFileType(file.getContentType());
            sysFile.setSuffix(suffix);
            sysFile.setSize(file.getSize());
            sysFile.setStorageType("RUSTFS");
            sysFile.setStorageUrl(objectName);
            sysFile.setBucketName(bucketName);
            sysFile.setObjectName(objectName);
            sysFile.setPreviewUrl(previewDomain + "/" + bucketName + "/" + objectName);
            sysFile.setVisitCount(0);
            sysFile.setCreateBy(userId);

            save(sysFile);

            log.info("文件上传成功: name={}, size={}, objectName={}", originalFilename, file.getSize(), objectName);

            return toResponse(sysFile);
        } catch (IOException e) {
            log.error("文件上传失败: name={}", originalFilename, e);
            throw new BusinessException(ResultCode.FAIL.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void download(Long id, HttpServletResponse response) {
        SysFile sysFile = getById(id);
        if (sysFile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文件不存在");
        }

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(sysFile.getBucketName())
                    .key(sysFile.getObjectName())
                    .responseContentDisposition("attachment; filename=\"" +
                            URLEncoder.encode(sysFile.getName(), StandardCharsets.UTF_8).replace("+", "%20") + "\"")
                    .build();

            ResponseInputStream<GetObjectResponse> objectStream = s3Client.getObject(getRequest);
            GetObjectResponse objectResponse = objectStream.response();

            // 设置响应头
            response.setContentType(objectResponse.contentType() != null ? objectResponse.contentType() : "application/octet-stream");
            response.setContentLengthLong(objectResponse.contentLength());
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(sysFile.getName(), StandardCharsets.UTF_8).replace("+", "%20") + "\"");

            // 写入输出流
            ServletOutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = objectStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            // 更新访问次数
            updateVisitCount(id);

            log.info("文件下载成功: id={}, name={}", id, sysFile.getName());
        } catch (IOException e) {
            log.error("文件下载失败: id={}, name={}", id, sysFile.getName(), e);
            throw new BusinessException(ResultCode.FAIL.getCode(), "文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileResponse> listFiles(Long userId) {
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SysFile::getCreateBy, userId);
        }
        wrapper.orderByDesc(SysFile::getCreateTime);

        List<SysFile> files = list(wrapper);
        return files.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id, Long userId) {
        SysFile sysFile = getById(id);
        if (sysFile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文件不存在");
        }

        // 从 RustFS 删除对象
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(sysFile.getBucketName())
                    .key(sysFile.getObjectName())
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("RustFS对象删除成功: bucket={}, key={}", sysFile.getBucketName(), sysFile.getObjectName());
        } catch (Exception e) {
            log.warn("RustFS对象删除失败（将继续删除数据库记录）: bucket={}, key={}", sysFile.getBucketName(), sysFile.getObjectName(), e);
        }

        // 逻辑删除数据库记录
        sysFile.setUpdateBy(userId);
        updateById(sysFile);
        removeById(id);

        log.info("文件删除成功: id={}, name={}", id, sysFile.getName());
    }

    // ========== 私有方法 ==========

    /**
     * 确保存储桶存在，不存在则创建
     */
    private void ensureBucketExists() {
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder().bucket(bucketName).build();
            s3Client.headBucket(headRequest);
        } catch (NoSuchBucketException e) {
            log.info("存储桶不存在，自动创建: bucket={}", bucketName);
            tryCreateBucket();
        } catch (S3Exception e) {
            // RustFS 等 S3 兼容存储可能在桶不存在时返回 400 Bad Request 而非 404
            if (e.statusCode() == 400) {
                log.info("存储桶可能不存在（headBucket返回400），尝试创建: bucket={}", bucketName);
                tryCreateBucket();
            } else {
                log.error("检查存储桶失败: bucket={}, statusCode={}", bucketName, e.statusCode(), e);
                throw e;
            }
        }
    }

    /**
     * 尝试创建存储桶，失败不中断流程
     * <p>
     * RustFS 等 S3 兼容存储在 createBucket 时也可能返回 400，
     * 此时无法区分「桶已存在」还是「创建失败」，因此由本方法兜底，
     * 让后续 putObject 操作做最终的存在性校验。
     */
    private void tryCreateBucket() {
        try {
            createBucket();
        } catch (Exception ex) {
            log.warn("创建存储桶失败，将直接尝试上传（桶可能已存在或稍后由管理员创建）: bucket={}, error={}",
                    bucketName, ex.getMessage());
        }
    }
    
    /**
     * 创建存储桶并设置公开读策略
     */
    private void createBucket() {
        try {
            CreateBucketRequest createRequest = CreateBucketRequest.builder().bucket(bucketName).build();
            s3Client.createBucket(createRequest);
            log.info("存储桶创建成功: bucket={}", bucketName);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            log.info("存储桶已存在，跳过创建: bucket={}", bucketName);
        }
    
        // 设置桶为公开读（允许预览）
        // 注意：仅当 createBucket 未抛异常（即桶创建成功或已存在）时才会执行
        try {
            if (bucketName == null || bucketName.isEmpty()) {
                log.warn("bucketName 为空，跳过设置桶策略");
                return;
            }
            // RustFS 等 S3 兼容存储在 policy 中资源 ARN 格式可能与 AWS 有差异，
            // 此处使用通用的 arn:aws:s3::: 前缀格式
            String resource = String.format("arn:aws:s3:::%s/*", bucketName);
            String policy = String.format(
                    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"],\"Resource\":[\"%s\"]}]}",
                    resource);
            log.debug("设置桶策略: bucket={}, policy={}", bucketName, policy);
            PutBucketPolicyRequest policyRequest = PutBucketPolicyRequest.builder()
                    .bucket(bucketName)
                    .policy(policy)
                    .build();
            s3Client.putBucketPolicy(policyRequest);
            log.info("存储桶设置为公开读: bucket={}", bucketName);
        } catch (Exception ex) {
            log.warn("设置存储桶公开读失败: bucket={}", bucketName, ex);
        }
    }

    /**
     * 获取文件后缀
     */
    private String getSuffix(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : "";
    }

    /**
     * 更新文件访问次数
     */
    private void updateVisitCount(Long id) {
        SysFile sysFile = getById(id);
        if (sysFile != null) {
            sysFile.setVisitCount((sysFile.getVisitCount() != null ? sysFile.getVisitCount() : 0) + 1);
            updateById(sysFile);
        }
    }

    /**
     * SysFile转FileResponse
     */
    private FileResponse toResponse(SysFile sysFile) {
        FileResponse response = new FileResponse();
        response.setId(sysFile.getId());
        response.setName(sysFile.getName());
        response.setFileType(sysFile.getFileType());
        response.setSuffix(sysFile.getSuffix());
        response.setSize(sysFile.getSize());
        response.setStorageType(sysFile.getStorageType());
        response.setStorageUrl(sysFile.getStorageUrl());
        response.setBucketName(sysFile.getBucketName());
        response.setPreviewUrl(sysFile.getPreviewUrl());
        response.setVisitCount(sysFile.getVisitCount());
        response.setCreateBy(sysFile.getCreateBy());
        response.setCreateTime(sysFile.getCreateTime());
        return response;
    }
}
