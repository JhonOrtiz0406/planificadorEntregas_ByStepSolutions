package tech.bystep.planificador.firebase;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.cloud.StorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.bystep.planificador.model.gateways.StorageGateway;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseStorageAdapter implements StorageGateway {

    @Value("${app.firebase.storage-bucket}")
    private String storageBucket;

    @Override
    public String uploadFile(String fileName, String contentType, byte[] bytes) {
        BlobId blobId = BlobId.of(storageBucket, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setAcl(List.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                .build();
        StorageClient.getInstance().bucket(storageBucket).getStorage().create(blobInfo, bytes);
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://storage.googleapis.com/" + storageBucket + "/" + encodedName;
    }
}
