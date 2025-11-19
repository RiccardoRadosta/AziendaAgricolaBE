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
     * Carica un file su Firebase Storage e restituisce l'URL pubblico.
     *
     * @param file Il file da caricare.
     * @return L'URL pubblico del file caricato.
     * @throws IOException Se si verifica un errore di I/O durante il caricamento.
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // Genera un nome di file univoco per evitare sovrascritture
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        
        Blob blob = storage.create(blobInfo, file.getBytes());

        // Costruisce l'URL pubblico secondo la convenzione di Firebase Storage
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
    }
}
