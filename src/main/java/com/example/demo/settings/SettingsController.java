
package com.example.demo.settings;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingService settingService;

    public SettingsController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublicSettings() {
        try {
            Setting settings = settingService.getSettings();
            Map<String, Object> publicSettings = Map.of(
                "standardShippingCost", settings.getStandardShippingCost(),
                "freeShippingThreshold", settings.getFreeShippingThreshold(),
                "splitShippingCost", settings.getSplitShippingCost() // Aggiunto il nuovo campo, usando Map.of
            );
            return ResponseEntity.ok(publicSettings);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving public settings", e);
        }
    }

    private Setting getSettingsInternal() {
        try {
            return settingService.getSettings();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore nel recuperare le impostazioni", e);
        }
    }

}
