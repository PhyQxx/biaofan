package com.biaofan.controller;

import com.biaofan.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    @Value("${spring.servlet.multipart.location:/www/sop-uploads}")
    private String uploadDir;

    /**
     * POST /api/upload/image
     * 接收 multipartFile，上传到服务器本地目录 /www/sop-uploads/YYYYMMDD/
     * 返回 {code: 0, data: {url: "/uploads/20260405/xxx.jpg"}}
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
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
            String dateDir = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String fullDir = uploadDir + File.separator + dateDir;
            Files.createDirectories(Paths.get(fullDir));

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;

            // 保存文件
            Path filePath = Paths.get(fullDir, filename);
            Files.copy(file.getInputStream(), filePath);

            // 返回相对路径 URL
            String url = "/uploads/" + dateDir + "/" + filename;
            return Result.ok(Map.of("url", url));

        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail(500, "文件上传失败: " + e.getMessage());
        }
    }
}
