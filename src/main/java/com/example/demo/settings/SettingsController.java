package com.example.demo.settings;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class SettingsController {

    private final SettingService settingService;

    public SettingsController(SettingService settingService) {
        this.settingService = settingService;
    }

    private Setting getSettingsInternal() {
        try {
            return settingService.getSettings();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore nel recuperare le impostazioni", e);
        }
    }

    // === ENDPOINT PUBBLICO ===

    @GetMapping("/settings")
    public ResponseEntity<Setting> getPublicSettings() {
        return ResponseEntity.ok(getSettingsInternal());
    }

    // === ENDPOINT ADMIN ===

    @GetMapping("/admin/settings")
    public ResponseEntity<Setting> getAdminSettings() {
        return ResponseEntity.ok(getSettingsInternal());
    }

    @PutMapping("/admin/settings")
    public ResponseEntity<Void> updateSettings(@RequestBody Setting settings) {
        try {
            settingService.saveSettings(settings);
            return ResponseEntity.ok().build();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore nel salvare le impostazioni", e);
        }
    }
}
