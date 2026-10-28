package com.luoxianjun.community.module.file.service;

import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.config.StorageProperties;
import com.luoxianjun.community.module.file.dto.UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 图片存储。
 * <p>
 * 三条硬规则：
 * <ol>
 *   <li><b>不信任 Content-Type</b>：客户端可随意伪造，一律读文件头按魔数判定真实类型；</li>
 *   <li><b>不信任原始文件名</b>：只用它判断扩展名都嫌脏，文件名完全由服务端生成 UUID，
 *       从根上消除 {@code ../../evil.jsp} 这类路径穿越；</li>
 *   <li><b>按年月分目录</b>：单目录堆几万个文件会让 ext4/NTFS 的目录查找退化。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final StorageProperties props;

    private static final DateTimeFormatter SUB_DIR_FMT = DateTimeFormatter.ofPattern("yyyy/MM");

    /** WEBP 的 RIFF 容器标识，位于文件第 8 字节 */
    private static final int[] WEBP_TAG = {'W', 'E', 'B', 'P'};

    /** 只允许图片：contentType + 扩展名由魔数反推，不接受客户端声明 */
    private enum ImageType {
        PNG("image/png", "png", new int[]{0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, 0),
        JPEG("image/jpeg", "jpg", new int[]{0xFF, 0xD8, 0xFF}, 0),
        GIF("image/gif", "gif", new int[]{'G', 'I', 'F', '8'}, 0),
        WEBP("image/webp", "webp", new int[]{'R', 'I', 'F', 'F'}, 0);

        final String contentType;
        final String ext;
        final int[] magic;
        final int offset;

        ImageType(String contentType, String ext, int[] magic, int offset) {
            this.contentType = contentType;
            this.ext = ext;
            this.magic = magic;
            this.offset = offset;
        }
    }

    public UploadResult store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > props.getMaxSizeBytes()) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }

        byte[] head = readHead(file, 12);
        ImageType type = detect(head);
        if (type == null) {
            throw new BizException(ErrorCode.FILE_TYPE_UNSUPPORTED);
        }
        // WEBP 除了 RIFF 头还要看第 8 字节起是不是 WEBP，否则 .wav/.avi 也会被当成图片
        if (type == ImageType.WEBP && !matches(head, WEBP_TAG, 8)) {
            throw new BizException(ErrorCode.FILE_TYPE_UNSUPPORTED);
        }

        String subDir = LocalDate.now().format(SUB_DIR_FMT);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + type.ext;

        Path root = Paths.get(props.getUploadDir()).toAbsolutePath().normalize();
        Path targetDir = root.resolve(subDir).normalize();
        // 双保险：子目录由服务端生成，理论上不会越界；仍校验一次，防后续改动引入漏洞
        if (!targetDir.startsWith(root)) {
            throw new BizException(ErrorCode.FILE_SAVE_FAILED);
        }

        try {
            Files.createDirectories(targetDir);
            // 不用 transferTo：它在某些容器下会把文件落到临时目录，且不接受 UUID 之外的路径控制
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetDir.resolve(filename));
            }
        } catch (IOException e) {
            log.error("保存文件失败 dir={} name={}", targetDir, filename, e);
            throw new BizException(ErrorCode.FILE_SAVE_FAILED);
        }

        String url = props.getPublicPrefix() + "/" + subDir + "/" + filename;
        log.info("文件已保存 {} ({} bytes, {})", url, file.getSize(), type.contentType);
        return new UploadResult(url, filename, file.getSize(), type.contentType);
    }

    /** 读文件头。文件可能很小（例如 1 字节），按实际长度返回 */
    private byte[] readHead(MultipartFile file, int len) {
        byte[] buf = new byte[len];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(buf, 0, len);
            if (read < buf.length) {
                byte[] trimmed = new byte[read];
                System.arraycopy(buf, 0, trimmed, 0, read);
                return trimmed;
            }
            return buf;
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_SAVE_FAILED);
        }
    }

    private ImageType detect(byte[] head) {
        for (ImageType t : ImageType.values()) {
            if (head.length >= t.offset + t.magic.length && matches(head, t.magic, t.offset)) {
                return t;
            }
        }
        return null;
    }

    private boolean matches(byte[] data, int[] magic, int offset) {
        if (data.length < offset + magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if ((data[offset + i] & 0xFF) != (magic[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
