package com.luoxianjun.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置。
 * <p>
 * 演示场景用本地磁盘（零外部依赖）；生产可换 OSS / S3，只需替换 FileStorageService 的实现。
 */
@Data
@Component
@ConfigurationProperties(prefix = "community.storage")
public class StorageProperties {

    /** 落盘根目录，相对路径以进程工作目录为基准 */
    private String uploadDir = "./data/uploads";

    /**
     * 对外访问前缀。必须与 server.servlet.context-path 对齐，
     * 否则返回的 URL 前端拼不出可用的地址。
     */
    private String publicPrefix = "/api/uploads";

    /** 单文件大小上限（字节），与 spring.servlet.multipart.max-file-size 保持一致 */
    private long maxSizeBytes = 5 * 1024 * 1024L;
}
