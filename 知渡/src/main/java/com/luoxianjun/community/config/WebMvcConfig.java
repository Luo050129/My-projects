package com.luoxianjun.community.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * 静态资源映射：把磁盘上的上传目录挂到 /uploads/** 下。
 * <p>
 * 注意 context-path 是 /api，所以实际对外地址是 /api/uploads/**，
 * 与 StorageProperties.publicPrefix 必须一致。
 * <p>
 * 缓存策略：文件名是 UUID，内容永不变化，可以放心让浏览器长期缓存。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable());
    }
}
