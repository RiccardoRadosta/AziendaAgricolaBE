package com.example.demo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account.key-path}")
    private String serviceAccountKeyPath;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        ClassPathResource resource = new ClassPathResource(serviceAccountKeyPath);
        try (InputStream serviceAccountStream = resource.getInputStream()) {
            return GoogleCredentials.fromStream(serviceAccountStream);
        }
    }

    @Bean
    public FirebaseApp firebaseApp(GoogleCredentials credentials) throws IOException {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    @Bean
    public Storage storage(GoogleCredentials credentials, @Value("${firebase.storage.bucket-name}") String bucketName) throws IOException {
         return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }
}
