package com.example.demo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    /**
     * Carica le credenziali Google dall'ambiente (tramite la variabile
     * GOOGLE_APPLICATION_CREDENTIALS). Necessario per Firestore.
     */
    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        return GoogleCredentials.getApplicationDefault();
    }

    /**
     * Inizializza l'app Firebase principale, usando le credenziali caricate.
     * La configurazione per Storage è stata rimossa perché non richiesta.
     */
    @Bean
    public FirebaseApp firebaseApp(GoogleCredentials credentials) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    /**
     * Fornisce il Bean per Firestore, usato dai servizi esistenti come OrderService.
     */
    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }
}
