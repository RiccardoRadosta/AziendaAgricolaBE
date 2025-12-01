package com.example.demo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    // Inject the full JSON content from an environment variable.
    // Spring Boot automatically maps a GOOGLE_CREDENTIALS_JSON env var to this field.
    // The ":" provides a default empty value if the env var is not set, ensuring the app starts.
    @Value("${google.credentials.json:}")
    private String gcpCredentialsJson;

    @Bean
    public Firestore firestore() throws IOException {
        GoogleCredentials credentials;

        // Priority 1: Use the explicit JSON content if provided via environment variable.
        // This is the key for making it work on Cloud Run correctly.
        if (gcpCredentialsJson != null && !gcpCredentialsJson.isEmpty()) {
            InputStream serviceAccountStream = new ByteArrayInputStream(gcpCredentialsJson.getBytes(StandardCharsets.UTF_8));
            credentials = GoogleCredentials.fromStream(serviceAccountStream);
        } else {
            // Priority 2 (Fallback): Use Application Default Credentials.
            // This will automatically pick up GOOGLE_APPLICATION_CREDENTIALS on your local machine,
            // or the Compute Engine service account if running on GCP without the var above.
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                .setCredentials(credentials);

        // The project ID is read directly from the credential (either the JSON content or the file).
        // So we don't need to set it manually anymore.

        FirebaseOptions options = optionsBuilder.build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }

        return FirestoreClient.getFirestore();
    }
}
