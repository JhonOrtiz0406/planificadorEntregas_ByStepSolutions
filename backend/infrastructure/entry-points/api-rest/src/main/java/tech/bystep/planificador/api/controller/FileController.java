package tech.bystep.planificador.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tech.bystep.planificador.api.dto.response.ApiResponse;
import tech.bystep.planificador.model.gateways.StorageGateway;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageGateway storageGateway;

    // Cámaras Android suelen devolver el archivo del intent de captura con
    // contentType null o "application/octet-stream" aunque sea un JPG real.
    // Si el header no trae "image/*", se cae a la extensión del archivo.
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp");

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "orders") String folder) throws IOException {
        String contentType = file.getContentType();
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String extNoDot = ext.startsWith(".") ? ext.substring(1).toLowerCase(Locale.ROOT) : ext.toLowerCase(Locale.ROOT);

        boolean validMime = contentType != null && contentType.startsWith("image/");
        boolean validExt = IMAGE_EXTENSIONS.contains(extNoDot);
        if (!validMime && !validExt) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Solo se permiten archivos de imagen"));
        }
        if (!validMime) {
            contentType = guessContentTypeFromExtension(extNoDot);
        }

        String fileName = folder + "/" + UUID.randomUUID() + ext;
        String url = storageGateway.uploadFile(fileName, contentType, file.getBytes());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("url", url)));
    }

    private String guessContentTypeFromExtension(String extNoDot) {
        return switch (extNoDot) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "heic" -> "image/heic";
            case "heif" -> "image/heif";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }

    @PostMapping("/upload/logo")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadLogo(
            @RequestParam("file") MultipartFile file) throws IOException {
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String fileName = "logos/" + UUID.randomUUID() + ext;
        String url = storageGateway.uploadFile(fileName, file.getContentType(), file.getBytes());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("url", url)));
    }
}
