package tech.bystep.planificador.firebase;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.bystep.planificador.model.gateways.StorageGateway;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseStorageAdapter implements StorageGateway {

    @Value("${app.firebase.storage-bucket}")
    private String storageBucket;

    @Override
    public String uploadFile(String fileName, String contentType, byte[] bytes) {
        Storage storage = StorageClient.getInstance().bucket(storageBucket).getStorage();

        String downloadToken = UUID.randomUUID().toString();

        BlobId blobId = BlobId.of(storageBucket, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setCacheControl("public, max-age=31536000, immutable")
                // Token embebido en metadata — hace el archivo accesible sin IAM público
                .setMetadata(Map.of("firebaseStorageDownloadTokens", downloadToken))
                .build();

        storage.create(blobInfo, bytes);

        // Firebase Storage REST URL — acceso vía token, sin configuración IAM
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://firebasestorage.googleapis.com/v0/b/"
                + storageBucket + "/o/" + encodedName
                + "?alt=media&token=" + downloadToken;
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            // URL format: .../o/{encoded-path}?alt=media&token=...
            int oIdx = fileUrl.indexOf("/o/");
            if (oIdx == -1) { log.warn("Cannot parse Firebase URL for deletion: {}", fileUrl); return; }
            String afterO = fileUrl.substring(oIdx + 3);
            String encodedPath = afterO.contains("?") ? afterO.substring(0, afterO.indexOf("?")) : afterO;
            String filePath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8);
            Storage storage = StorageClient.getInstance().bucket(storageBucket).getStorage();
            storage.delete(BlobId.of(storageBucket, filePath));
            log.info("Deleted file from Firebase Storage: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to delete file from Firebase Storage: {} — {}", fileUrl, e.getMessage());
        }
    }
}
