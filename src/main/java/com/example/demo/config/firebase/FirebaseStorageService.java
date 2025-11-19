package com.example.demo.config.firebase;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseStorageService {

    private final Storage storage;
    private final String bucketName;

    @Autowired
    public FirebaseStorageService(Storage storage, @Value("${firebase.storage.bucket-name}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();
        Blob blob = storage.create(blobInfo, file.getBytes());

        // Genera un URL pubblico leggibile
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
    }

    public void deleteFileFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            // Estrai il nome del bucket e il nome dell'oggetto dall'URL
            String prefix = String.format("https://storage.googleapis.com/%s/", this.bucketName);
            if (fileUrl.startsWith(prefix)) {
                String objectName = fileUrl.substring(prefix.length());
                storage.delete(this.bucketName, objectName);
                System.out.println("Successfully deleted file: " + objectName);
            }
        } catch (Exception e) {
            // Logga l'errore ma non bloccare l'esecuzione
            System.err.println("Error deleting file from Firebase Storage: " + fileUrl + " | Error: " + e.getMessage());
        }
    }
}
