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

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private final String projectId;
    private final String bucketName;

    public FirebaseConfig(
            @Value("${google.storage.project-id}") String projectId,
            @Value("${firebase.storage.bucket-name}") String bucketName) {
        this.projectId = projectId;
        this.bucketName = bucketName;
    }

    /**
     * Crea un Bean per le credenziali Google, caricandole una sola volta.
     * Questo bean verrà poi iniettato negli altri servizi Firebase.
     */
    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        return GoogleCredentials.getApplicationDefault();
    }

    /**
     * Crea il Bean per l'applicazione Firebase principale, usando le credenziali già caricate.
     */
    @Bean
    public FirebaseApp firebaseApp(GoogleCredentials credentials) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setStorageBucket(bucketName)
                    .build();
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    /**
     * Crea il Bean per Firestore.
     */
    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    /**
     * Crea il Bean per Firebase Storage, riutilizzando le stesse credenziali dell'app.
     */
    @Bean
    public Storage storage(GoogleCredentials credentials) throws IOException {
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}
