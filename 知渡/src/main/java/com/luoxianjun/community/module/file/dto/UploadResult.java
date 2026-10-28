package com.luoxianjun.community.module.file.dto;

/**
 * 上传结果。url 为可直接放进 img src 的相对路径，前端无需再拼域名。
 *
 * @param url         访问路径，如 /api/uploads/2026/10/xxx.png
 * @param name        文件名（服务端生成，与原始文件名无关）
 * @param size        字节数
 * @param contentType 探测出的真实类型（非客户端声明）
 */
public record UploadResult(String url, String name, long size, String contentType) {
}
