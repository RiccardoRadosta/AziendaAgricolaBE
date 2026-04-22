package com.example.demo.bundle;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class BundleController {

    private final BundleService bundleService;

    public BundleController(BundleService bundleService) {
        this.bundleService = bundleService;
    }

    // Endpoint pubblico
    @GetMapping("/bundles/active")
    public ResponseEntity<List<Bundle>> getActiveBundles() {
        try {
            return ResponseEntity.ok(bundleService.getActiveBundles());
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoints Admin
    @GetMapping("/admin/bundles")
    public ResponseEntity<List<Bundle>> getAllBundles() {
        try {
            return ResponseEntity.ok(bundleService.getAllBundles());
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/admin/bundles")
    public ResponseEntity<?> createBundle(@RequestBody BundleDTO bundleDTO) {
        try {
            String id = bundleService.createBundle(bundleDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/admin/bundles/{id}")
    public ResponseEntity<?> updateBundle(@PathVariable String id, @RequestBody BundleDTO bundleDTO) {
        try {
            bundleService.updateBundle(id, bundleDTO);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/admin/bundles/{id}")
    public ResponseEntity<?> deleteBundle(@PathVariable String id) {
        try {
            bundleService.deleteBundle(id);
            return ResponseEntity.noContent().build();
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
