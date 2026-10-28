package com.luoxianjun.community.module.file.controller;

import com.luoxianjun.community.common.annotation.RateLimit;
import com.luoxianjun.community.common.result.Result;
import com.luoxianjun.community.module.file.dto.UploadResult;
import com.luoxianjun.community.module.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传。需登录，并做接口级限流——上传是唯一会写磁盘的入口，被刷等于被占满硬盘。
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(key = "upload", window = 60, max = 20)
    public Result<UploadResult> upload(@RequestPart("file") MultipartFile file) {
        return Result.success(fileStorageService.store(file));
    }
}
