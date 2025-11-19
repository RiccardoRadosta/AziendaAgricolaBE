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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class FirebaseStorageService {

    private final Storage storage;
    private final String bucketName;

    @Autowired
    public FirebaseStorageService(Storage storage, @Value("${firebase.storage.bucket-name}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    /**
     * Carica un file su Firebase Storage usando un nome file specifico.
     *
     * @param file Il file da caricare.
     * @param fileName Il nome (e percorso) completo da assegnare al file nello storage.
     * @return L'URL pubblico per accedere al file.
     * @throws IOException Se si verifica un errore di I/O.
     */
    public String uploadFile(MultipartFile file, String fileName) throws IOException {
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        // L'URL pubblico deve essere costruito codificando il nome del file (percorso)
        return String.format("https://storage.googleapis.com/%s/%s",
                bucketName, URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()));
    }
}
