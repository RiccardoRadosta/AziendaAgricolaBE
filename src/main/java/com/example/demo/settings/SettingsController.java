
package com.example.demo.settings;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

            Map<String, Object> publicSettings = new HashMap<>();

            // Impostazioni Italia
            publicSettings.put("standardShippingCost", Optional.ofNullable(settings.getStandardShippingCost()).orElse(new BigDecimal("10")));
            publicSettings.put("freeShippingThreshold", Optional.ofNullable(settings.getFreeShippingThreshold()).orElse(BigDecimal.ZERO));
            publicSettings.put("splitShippingCost", Optional.ofNullable(settings.getSplitShippingCost()).orElse(BigDecimal.ZERO));

            // Impostazioni UE
            publicSettings.put("standardShippingCost_UE", Optional.ofNullable(settings.getStandardShippingCost_UE()).orElse(BigDecimal.ZERO));
            publicSettings.put("freeShippingThreshold_UE", Optional.ofNullable(settings.getFreeShippingThreshold_UE()).orElse(BigDecimal.ZERO));
            publicSettings.put("splitShippingCost_UE", Optional.ofNullable(settings.getSplitShippingCost_UE()).orElse(BigDecimal.ZERO));

            // Impostazioni Corriere
            publicSettings.put("NomeCorriere", Optional.ofNullable(settings.getNomeCorriere()).orElse(""));
            publicSettings.put("LinkTrackingPage", Optional.ofNullable(settings.getLinkTrackingPage()).orElse(""));

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
