package com.fnb.menu.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Delete an image from Cloudinary by its Public ID
     * @param publicId Public ID of the image (e.g. "items/burger")
     */
    @Async
    public void deleteImage(String input) {
        if (input == null || input.isEmpty()) return;

        String publicId = input;
        if (input.startsWith("http")) {
            // Nếu là URL, trích xuất Public ID
            publicId = extractPublicId(input);
        }

        if (publicId == null || publicId.isEmpty()) {
            log.warn("Could not determine publicId from input: {}", input);
            return;
        }

        try {
            // URL có thể bị encode (đặc biệt là tên file tiếng Việt), cần decode trước khi gửi lên Cloudinary
            publicId = java.net.URLDecoder.decode(publicId, java.nio.charset.StandardCharsets.UTF_8.name());
            
            log.info("Deleting image from Cloudinary ID: [{}]", publicId);
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            
            if ("ok".equals(result.get("result"))) {
                log.info("Successfully deleted image from Cloudinary: [{}]", publicId);
            } else {
                log.warn("Cloudinary delete result for [{}]: {}", publicId, result);
            }
        } catch (Exception e) {
            log.error("Failed to delete image from Cloudinary [id={}]. Error message: {}. Cause: {}", publicId, e.getMessage(), e.getCause());
            // Nếu có lỗi parse JSON, log thêm để check (có thể do sai cloud_name)
            if (e.getMessage() != null && e.getMessage().contains("JSONObject")) {
                log.error("HINT: This error often happens when Cloudinary credentials (cloud-name) are incorrect or missing, causing an HTML error response instead of JSON.");
            }
        }
    }

    /**
     * Extracts publicId from Cloudinary URL
     * Example: https://res.cloudinary.com/demo/image/upload/v12345/folder/sample.jpg -> folder/sample
     */
    public String extractPublicId(String url) {
        if (url == null || !url.contains("/upload/")) return null;

        try {
            // 1. Lấy phần sau /upload/
            String afterUpload = url.split("/upload/")[1];
            
            // 2. Tách các segment bằng dấu /
            String[] segments = afterUpload.split("/");
            
            // 3. Tìm tập hợp các segment tạo nên public_id
            // Bỏ qua các segment là transformation (chứa dấu , hoặc các ký tự đặc biệt) 
            // và bỏ qua version (bắt đầu bằng 'v' + số)
            StringBuilder publicIdBuilder = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                String s = segments[i];
                
                // Kiểm tra nếu là version (vd: v1744359654)
                if (s.matches("v\\d+") && i < segments.length - 1) {
                    continue;
                }
                
                // Kiểm tra nếu là transformation (thường chứa dấu phẩy , hoặc dấu gạch dưới _)
                // Lưu ý: Tên folder cũng có thể chứa _, nhưng transformation thường có cấu trúc đặc thù
                if (s.contains(",") || (s.matches("[a-z]_[a-z0-9]+.*") && i < segments.length - 1)) {
                    continue;
                }
                
                if (publicIdBuilder.length() > 0) {
                    publicIdBuilder.append("/");
                }
                publicIdBuilder.append(s);
            }

            String fullPath = publicIdBuilder.toString();
            
            // 4. Loại bỏ phần mở rộng file (extension) ở cuối
            int lastDot = fullPath.lastIndexOf(".");
            if (lastDot != -1) {
                return fullPath.substring(0, lastDot);
            }
            return fullPath;
        } catch (Exception e) {
            log.warn("Could not extract publicId from URL: {}. Error: {}", url, e.getMessage());
            return null;
        }
    }
}
