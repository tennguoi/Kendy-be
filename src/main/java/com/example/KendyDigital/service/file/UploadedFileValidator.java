package com.example.KendyDigital.service.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UploadedFileValidator {
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "pdf", "txt", "csv", "xlsx");
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "gif", Set.of("image/gif"),
            "webp", Set.of("image/webp"),
            "pdf", Set.of("application/pdf"),
            "txt", Set.of("text/plain"),
            "csv", Set.of("text/csv", "application/csv", "text/plain"),
            "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/zip"));

    public ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "File must not exceed 10 MiB");
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File type is not allowed");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.get(extension).contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Content type does not match the file extension");
        }

        try {
            byte[] content = file.getBytes();
            if (!hasExpectedSignature(extension, content)) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "File content does not match the declared type");
            }
            return new ValidatedUpload(fileName, contentType, content);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read uploaded file");
        }
    }

    private String sanitizeFileName(String originalName) {
        String name = originalName == null ? "" : originalName.trim();
        if (name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        String sanitized = name.replaceAll("[^A-Za-z0-9._ -]", "_");
        if (sanitized.isBlank() || sanitized.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        return sanitized;
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean hasExpectedSignature(String extension, byte[] content) {
        return switch (extension) {
            case "jpg", "jpeg" -> startsWith(content, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> startsWithAscii(content, "GIF87a") || startsWithAscii(content, "GIF89a");
            case "webp" -> startsWithAscii(content, "RIFF")
                    && content.length >= 12
                    && new String(content, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            case "pdf" -> startsWithAscii(content, "%PDF-");
            case "xlsx" -> startsWith(content, 0x50, 0x4B, 0x03, 0x04);
            case "txt", "csv" -> isSafeText(content);
            default -> false;
        };
    }

    private boolean isSafeText(byte[] content) {
        int inspected = Math.min(content.length, 4096);
        for (int index = 0; index < inspected; index++) {
            if (content[index] == 0) {
                return false;
            }
        }
        String prefix = new String(content, 0, inspected, StandardCharsets.UTF_8)
                .stripLeading()
                .toLowerCase(Locale.ROOT);
        return !prefix.startsWith("#!")
                && !prefix.startsWith("<script")
                && !prefix.startsWith("<html")
                && !prefix.startsWith("<!doctype html")
                && !prefix.startsWith("mz");
    }

    private boolean startsWithAscii(byte[] content, String value) {
        return startsWith(content, value.getBytes(StandardCharsets.US_ASCII));
    }

    private boolean startsWith(byte[] content, int... expected) {
        if (content.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if ((content[index] & 0xFF) != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWith(byte[] content, byte[] expected) {
        if (content.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if (content[index] != expected[index]) {
                return false;
            }
        }
        return true;
    }

    public record ValidatedUpload(String fileName, String contentType, byte[] content) {
        public long sizeBytes() {
            return content.length;
        }
    }
}
