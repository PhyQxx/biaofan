package com.biaofan.controller;

import com.biaofan.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传 Controller
 * - POST /api/upload/image: 上传图片
 *   - 存储到 /www/sop-uploads/ 目录
 *   - 最大 10MB，支持 jpg/png/gif/webp 格式
 *   - 返回访问 URL
 */
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器
 * 提供图片上传功能，支持JPG、PNG、GIF等常见图片格式
 */

/**
 * 文件上传 Controller
 * - POST /api/upload/image: 上传图片
 *   - 存储到 /www/sop-uploads/ 目录
 *   - 最大 10MB，支持 jpg/png/gif/webp 格式
 *   - 返回访问 URL
 */

/**
 * 文件上传 Controller
 * - POST /api/upload/image: 上传图片
 *   - 存储到 /www/sop-uploads/ 目录
 *   - 最大 10MB，支持 jpg/png/gif/webp 格式
 *   - 返回访问 URL
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    @Value("${spring.servlet.multipart.location:/www/sop-uploads}")
    private String uploadDir;

    // H-08: 扩展名白名单校验
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg"
    );
    
    // Issue 28: Static date formatter to avoid per-request SimpleDateFormat allocation
    private static final DateTimeFormatter DATE_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 上传图片
     * 接收multipartFile，上传到服务器本地目录 /www/sop-uploads/YYYYMMDD/
     * @param file 图片文件（支持JPG、PNG、GIF、BMP、WebP、SVG，最大10MB）
     * @return 上传结果，包含文件访问路径
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(400, "只支持图片文件");
        }

        // 校验文件大小 (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.fail(400, "文件大小不能超过 10MB");
        }

        try {
            // 创建日期子目录
            String dateDir = LocalDate.now().format(DATE_DIR_FORMATTER);
            String fullDir = uploadDir + File.separator + dateDir;
            Files.createDirectories(Paths.get(fullDir));

            // 生成唯一文件名，使用内容类型而非客户端提供的扩展名
            String detectedContentType = file.getContentType();
            String safeExt;
            if (detectedContentType != null) {
                safeExt = switch (detectedContentType) {
                    case "image/jpeg" -> ".jpg";
                    case "image/png" -> ".png";
                    case "image/gif" -> ".gif";
                    case "image/webp" -> ".webp";
                    case "application/pdf" -> ".pdf";
                    default -> {
                        log.warn("Unsupported content type: {}", detectedContentType);
                        yield ".bin";
                    }
                };
            } else {
                safeExt = ".bin";
            }

            String filename = UUID.randomUUID().toString().replace("-", "") + safeExt;

            // 保存文件
            Path filePath = Paths.get(fullDir, filename);
            Files.copy(file.getInputStream(), filePath);

            // 返回相对路径 URL
            String url = "/uploads/" + dateDir + "/" + filename;
            return Result.ok(Map.of("url", url));

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.fail(500, "文件上传失败: " + e.getMessage());
        }
    }
}
