package com.company.vehicle.service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UploadStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp", ".bmp", ".gif");
    private static final List<String> THUMBNAIL_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".bmp");
    private static final String UPLOADS_PREFIX = "/uploads/";
    private static final String THUMB_SUFFIX = "_thumb";
    private static final int THUMB_MAX_DIMENSION = 480;

    public String saveImageFiles(MultipartFile[] files, String fieldLabel) {
        if (files == null || files.length == 0) {
            return "";
        }

        Path uploadDir = getUploadDir();
        List<String> savedPaths = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                validateImageFile(file, fieldLabel);
                savedPaths.add(saveFile(uploadDir, file));
            }
        } catch (RuntimeException exception) {
            savedPaths.forEach(this::deletePath);
            throw exception;
        }
        registerRollbackCleanup(savedPaths);
        return String.join(",", savedPaths);
    }

    public void deleteCsvPaths(String... csvValues) {
        if (csvValues == null) {
            return;
        }
        for (String csv : csvValues) {
            if (csv == null || csv.isBlank()) {
                continue;
            }
            Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(path -> !path.isBlank())
                    .forEach(this::deletePath);
        }
    }

    private Path getUploadDir() {
        try {
            Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads");
            Files.createDirectories(uploadDir);
            return uploadDir;
        } catch (IOException e) {
            throw new IllegalStateException("创建上传目录失败", e);
        }
    }

    private void validateImageFile(MultipartFile file, String fieldLabel) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        String lowerName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        boolean validExtension = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (contentType == null || !contentType.startsWith("image/") || !validExtension) {
            throw new IllegalArgumentException(fieldLabel + "只支持图片文件上传");
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException(fieldLabel + "只支持图片文件上传");
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取上传图片失败", e);
        }
    }

    private String saveFile(Path uploadDir, MultipartFile file) {
        String filename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
        String savedPath = UPLOADS_PREFIX + filename;
        try {
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            generateThumbnailIfNeeded(target, filename);
            return savedPath;
        } catch (IOException | RuntimeException e) {
            deletePath(savedPath);
            throw new IllegalStateException("文件上传失败", e);
        }
    }

    private void generateThumbnailIfNeeded(Path originalFile, String filename) throws IOException {
        String extension = extractExtension(filename);
        if (!THUMBNAIL_EXTENSIONS.contains(extension)) {
            return;
        }

        BufferedImage sourceImage = ImageIO.read(originalFile.toFile());
        if (sourceImage == null) {
            return;
        }

        Path thumbnailFile = originalFile.resolveSibling(buildThumbnailFilename(filename));
        if (sourceImage.getWidth() <= THUMB_MAX_DIMENSION && sourceImage.getHeight() <= THUMB_MAX_DIMENSION) {
            Files.copy(originalFile, thumbnailFile, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        double scale = Math.min(
                (double) THUMB_MAX_DIMENSION / sourceImage.getWidth(),
                (double) THUMB_MAX_DIMENSION / sourceImage.getHeight()
        );
        int targetWidth = Math.max(1, (int) Math.round(sourceImage.getWidth() * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceImage.getHeight() * scale));

        BufferedImage thumbnailImage = resizeImage(sourceImage, targetWidth, targetHeight, ".png".equals(extension));
        boolean written = ImageIO.write(thumbnailImage, resolveThumbnailFormat(extension), thumbnailFile.toFile());
        if (!written) {
            throw new IllegalStateException("缩略图生成失败");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String lowerName = originalFilename.toLowerCase(Locale.ROOT);
        int lastDot = lowerName.lastIndexOf('.');
        if (lastDot < 0) {
            throw new IllegalArgumentException("文件必须有扩展名");
        }
        String extension = lowerName.substring(lastDot);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension + "，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        return extension;
    }

    public static String resolveThumbnailPath(String path) {
        String normalizedPath = normalizeUploadPath(path);
        if (!normalizedPath.startsWith(UPLOADS_PREFIX)) {
            return normalizedPath;
        }
        int extensionIndex = getExtensionIndex(normalizedPath);
        if (extensionIndex < 0) {
            return normalizedPath;
        }
        String extension = normalizedPath.substring(extensionIndex).toLowerCase(Locale.ROOT);
        String basename = normalizedPath.substring(0, extensionIndex).toLowerCase(Locale.ROOT);
        if (!THUMBNAIL_EXTENSIONS.contains(extension) || basename.endsWith(THUMB_SUFFIX)) {
            return normalizedPath;
        }
        return normalizedPath.substring(0, extensionIndex) + THUMB_SUFFIX + normalizedPath.substring(extensionIndex);
    }

    public static String resolveOriginalPathFromThumbnail(String path) {
        String normalizedPath = normalizeUploadPath(path);
        if (!normalizedPath.startsWith(UPLOADS_PREFIX)) {
            return normalizedPath;
        }
        int extensionIndex = getExtensionIndex(normalizedPath);
        if (extensionIndex < 0) {
            return normalizedPath;
        }
        String basename = normalizedPath.substring(0, extensionIndex);
        if (!basename.endsWith(THUMB_SUFFIX)) {
            return normalizedPath;
        }
        return basename.substring(0, basename.length() - THUMB_SUFFIX.length()) + normalizedPath.substring(extensionIndex);
    }

    public StoredImageResource resolveDriverPreview(String path) {
        String normalizedPath = normalizeUploadPath(path);
        if (!normalizedPath.startsWith(UPLOADS_PREFIX)) {
            throw new IllegalArgumentException("图片不存在");
        }

        Path previewPath = resolveExistingUploadFile(resolveThumbnailPath(normalizedPath));
        if (previewPath == null) {
            previewPath = resolveExistingUploadFile(normalizedPath);
        }
        if (previewPath == null) {
            throw new IllegalArgumentException("图片不存在");
        }

        try {
            String contentType = Files.probeContentType(previewPath);
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            return new StoredImageResource(previewPath, contentType, Files.size(previewPath));
        } catch (IOException e) {
            throw new IllegalStateException("读取图片失败", e);
        }
    }

    private void registerRollbackCleanup(List<String> savedPaths) {
        if (savedPaths.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    savedPaths.forEach(UploadStorageService.this::deletePath);
                }
            }
        });
    }

    private void deletePath(String relativePath) {
        deleteSinglePath(relativePath);

        String thumbnailPath = resolveThumbnailPath(relativePath);
        if (!thumbnailPath.equals(relativePath)) {
            deleteSinglePath(thumbnailPath);
        }

        String originalPath = resolveOriginalPathFromThumbnail(relativePath);
        if (!originalPath.equals(relativePath)) {
            deleteSinglePath(originalPath);
        }
    }

    private void deleteSinglePath(String relativePath) {
        try {
            String normalized = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
            Path filePath = Path.of(System.getProperty("user.dir"), normalized);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 记录错误但不中断流程
            System.err.println("删除文件失败: " + relativePath + ", 错误: " + e.getMessage());
        }
    }

    private static String normalizeUploadPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static int getExtensionIndex(String path) {
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        return lastDot > lastSlash ? lastDot : -1;
    }

    private String buildThumbnailFilename(String filename) {
        int extensionIndex = getExtensionIndex(filename);
        if (extensionIndex < 0) {
            return filename + THUMB_SUFFIX;
        }
        return filename.substring(0, extensionIndex) + THUMB_SUFFIX + filename.substring(extensionIndex);
    }

    private Path resolveExistingUploadFile(String uploadPath) {
        Path resolvedPath = resolveUploadPath(uploadPath);
        if (resolvedPath == null || !Files.isRegularFile(resolvedPath) || !Files.isReadable(resolvedPath)) {
            return null;
        }
        return resolvedPath;
    }

    private Path resolveUploadPath(String uploadPath) {
        String normalizedPath = normalizeUploadPath(uploadPath);
        if (!normalizedPath.startsWith(UPLOADS_PREFIX)) {
            return null;
        }

        Path uploadDir = getUploadDir();
        Path resolvedPath = uploadDir.resolve(normalizedPath.substring(UPLOADS_PREFIX.length())).normalize();
        return resolvedPath.startsWith(uploadDir) ? resolvedPath : null;
    }

    private BufferedImage resizeImage(BufferedImage sourceImage, int width, int height, boolean preserveAlpha) {
        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resizedImage = new BufferedImage(width, height, imageType);
        Graphics2D graphics = resizedImage.createGraphics();
        try {
            if (preserveAlpha) {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, width, height);
                graphics.setComposite(AlphaComposite.SrcOver);
            } else {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return resizedImage;
    }

    private String resolveThumbnailFormat(String extension) {
        if (".png".equals(extension)) {
            return "png";
        }
        if (".bmp".equals(extension)) {
            return "bmp";
        }
        return "jpg";
    }

    public record StoredImageResource(Path path, String contentType, long length) {
    }
}
