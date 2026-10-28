package com.luoxianjun.community.module.file;

import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.config.StorageProperties;
import com.luoxianjun.community.module.file.dto.UploadResult;
import com.luoxianjun.community.module.file.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件存储单测。重点不是「能存」，而是「不该存的存不进去」。
 */
class FileStorageServiceTest {

    @TempDir
    Path uploadRoot;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        StorageProperties props = new StorageProperties();
        props.setUploadDir(uploadRoot.toString());
        props.setPublicPrefix("/api/uploads");
        props.setMaxSizeBytes(5 * 1024 * 1024L);
        service = new FileStorageService(props);
    }

    /** 最小合法 PNG：8 字节魔数 + 少量数据，足够让魔数判定通过 */
    private byte[] pngBytes() {
        byte[] head = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] tail = {0, 0, 0, 13, 'I', 'H', 'D', 'R'};
        byte[] out = new byte[head.length + tail.length];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(tail, 0, out, head.length, tail.length);
        return out;
    }

    @Test
    @DisplayName("PNG 上传成功，返回可访问的相对路径且文件已落盘")
    void storePng() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "screenshot.png", "image/png", pngBytes());

        UploadResult result = service.store(file);

        assertTrue(result.url().startsWith("/api/uploads/"), "URL 应带公开前缀: " + result.url());
        assertTrue(result.url().endsWith(".png"));
        assertEquals("image/png", result.contentType());
        assertEquals(pngBytes().length, result.size());

        // URL 去掉前缀后应能在磁盘上找到对应文件
        String relative = result.url().substring("/api/uploads/".length());
        Path stored = uploadRoot.resolve(relative);
        assertTrue(Files.exists(stored), "文件应落盘: " + stored);
        assertArrayEquals(pngBytes(), Files.readAllBytes(stored));
    }

    @Test
    @DisplayName("客户端把文本文件声明成 image/png，按魔数识别后拒绝")
    void rejectFakeContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.png", "image/png",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        BizException ex = assertThrows(BizException.class, () -> service.store(file));
        assertEquals(ErrorCode.FILE_TYPE_UNSUPPORTED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("扩展名与真实类型不符时，按真实类型落盘（内容优先于声明）")
    void extensionFollowsRealType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", pngBytes());

        UploadResult result = service.store(file);

        assertTrue(result.url().endsWith(".png"), "应按 PNG 落盘，实际: " + result.url());
        assertEquals("image/png", result.contentType());
    }

    @Test
    @DisplayName("原始文件名中的路径穿越字符不影响落盘位置")
    void ignoreMaliciousFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../../etc/passwd.png", "image/png", pngBytes());

        UploadResult result = service.store(file);

        assertFalse(result.name().contains(".."), "落盘文件名应为服务端生成: " + result.name());
        assertTrue(result.name().matches("[0-9a-f]{32}\\.png"));
        // 所有落盘文件都必须在根目录的子目录里
        try (Stream<Path> walk = Files.walk(uploadRoot)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();
            assertEquals(1, files.size());
            assertTrue(files.get(0).normalize().startsWith(uploadRoot.normalize()));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("空文件被拒绝")
    void rejectEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        BizException ex = assertThrows(BizException.class, () -> service.store(file));
        assertEquals(ErrorCode.FILE_EMPTY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("超过大小上限被拒绝")
    void rejectTooLarge() {
        StorageProperties small = new StorageProperties();
        small.setUploadDir(uploadRoot.toString());
        small.setMaxSizeBytes(8);
        FileStorageService tiny = new FileStorageService(small);

        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", pngBytes());

        BizException ex = assertThrows(BizException.class, () -> tiny.store(file));
        assertEquals(ErrorCode.FILE_TOO_LARGE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("JPEG / GIF / WEBP 魔数均能识别")
    void recognizeOtherFormats() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
        byte[] gif = {'G', 'I', 'F', '8', '9', 'a', 0, 0};
        byte[] webp = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P'};

        assertEquals("image/jpeg", service.store(new MockMultipartFile("file", "a.jpg", "image/jpeg", jpeg)).contentType());
        assertEquals("image/gif", service.store(new MockMultipartFile("file", "b.gif", "image/gif", gif)).contentType());
        assertEquals("image/webp", service.store(new MockMultipartFile("file", "c.webp", "image/webp", webp)).contentType());
    }

    @Test
    @DisplayName("RIFF 开头但不是 WEBP（如 wav）不被当作图片")
    void rejectRiffNonWebp() {
        byte[] wav = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'A', 'V', 'E'};

        BizException ex = assertThrows(BizException.class,
                () -> service.store(new MockMultipartFile("file", "sound.wav", "audio/wav", wav)));
        assertEquals(ErrorCode.FILE_TYPE_UNSUPPORTED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("1 字节文件不会导致越界读取")
    void handleTinyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "tiny.png", "image/png", new byte[]{0x01});

        BizException ex = assertThrows(BizException.class, () -> service.store(file));
        assertEquals(ErrorCode.FILE_TYPE_UNSUPPORTED.getCode(), ex.getCode());
    }
}
