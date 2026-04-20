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
}
