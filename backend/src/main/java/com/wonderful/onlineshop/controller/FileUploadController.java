package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileUploadController {
    private static final Set<String> DESIGN_ALLOWED_EXTENSIONS = Set.of(".pdf", ".cdr", ".jpg", ".ai");
    private static final Set<String> CERT_ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".pdf");

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping({ "/upload", "/public-upload" }) // Keep original for backwards compat?
    public ApiResponse<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "scene", required = false) String scene) throws IOException {
        if (file.isEmpty()) {
            return ApiResponse.fail("文件为空");
        }

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String normalizedExt = ext.toLowerCase(Locale.ROOT);
        if ("design".equalsIgnoreCase(scene) && !DESIGN_ALLOWED_EXTENSIONS.contains(normalizedExt)) {
            return ApiResponse.fail("仅支持 .pdf .cdr .jpg .ai 格式");
        }
        if ("certificate".equalsIgnoreCase(scene) && !CERT_ALLOWED_EXTENSIONS.contains(normalizedExt)) {
            return ApiResponse.fail("证书仅支持 jpg/png/gif/pdf 格式");
        }
        String newFilename = UUID.randomUUID().toString() + ext;

        // Save file
        Path filePath = uploadPath.resolve(newFilename);
        file.transferTo(filePath.toAbsolutePath().toFile());

        // Return URL
        Map<String, String> data = new HashMap<>();
        data.put("url", "/uploads/" + newFilename);
        return ApiResponse.ok(data);
    }
}
