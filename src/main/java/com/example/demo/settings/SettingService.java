package com.example.demo.settings;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class SettingService {

    private final DocumentReference settingsDocRef;

    public SettingService(Firestore firestore) {
        // Usiamo un ID fisso per avere sempre un solo documento di impostazioni
        this.settingsDocRef = firestore.collection("settings").document(Setting.SINGLETON_ID);
    }

    public Setting getSettings() throws ExecutionException, InterruptedException {
        DocumentSnapshot document = settingsDocRef.get().get();
        if (document.exists()) {
            return document.toObject(Setting.class);
        } else {
            // Se non esistono impostazioni, ne restituisce di default (ma non le salva)
            return new Setting();
        }
    }

    public void saveSettings(Setting settings) throws ExecutionException, InterruptedException {
        settingsDocRef.set(settings).get();
    }
}
