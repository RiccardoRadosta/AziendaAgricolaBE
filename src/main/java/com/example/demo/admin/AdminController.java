package com.example.demo.admin;

import com.example.demo.admin.dto.DashboardStatsDTO;
import com.example.demo.admin.dto.NewsletterRequestDTO;
import com.example.demo.admin.dto.ShipmentListDTO;
import com.example.demo.newsletter.NewsletterService;
import com.example.demo.newsletter.NewsletterSubscriptionDTO;
import com.example.demo.order.BrevoEmailService;
import com.example.demo.order.Order;
import com.example.demo.order.OrderService;
import com.example.demo.security.JwtUtil;
import com.example.demo.settings.Setting;
import com.example.demo.settings.SettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JwtUtil jwtUtil;
    private final OrderService orderService;
    private final DashboardService dashboardService;
    private final CloudinaryService cloudinaryService;
    private final NewsletterService newsletterService;
    private final VercelAnalyticsService vercelAnalyticsService;
    private final SettingService settingService;
    private final ObjectMapper objectMapper;
    private final ExcelService excelService;
    private final BrevoEmailService brevoEmailService;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    public AdminController(JwtUtil jwtUtil, OrderService orderService, DashboardService dashboardService, CloudinaryService cloudinaryService, NewsletterService newsletterService, VercelAnalyticsService vercelAnalyticsService, SettingService settingService, ObjectMapper objectMapper, ExcelService excelService, BrevoEmailService brevoEmailService) {
        this.jwtUtil = jwtUtil;
        this.orderService = orderService;
        this.dashboardService = dashboardService;
        this.cloudinaryService = cloudinaryService;
        this.newsletterService = newsletterService;
        this.vercelAnalyticsService = vercelAnalyticsService;
        this.settingService = settingService;
        this.objectMapper = objectMapper;
        this.excelService = excelService;
        this.brevoEmailService = brevoEmailService;
    }

    @PostMapping("/send-invoice")
    public ResponseEntity<?> sendInvoice(
            @RequestParam("email") String email,
            @RequestParam("orderId") String orderId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (email == null || email.isEmpty() || orderId == null || orderId.isEmpty() || file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email, Order ID, and File are required."));
            }

            brevoEmailService.sendInvoiceEmail(email, orderId, file);
            return ResponseEntity.ok(Map.of("success", true, "message", "Fattura inviata con successo."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Errore durante l'invio della fattura: " + e.getMessage()));
        }
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportOrders(@RequestBody Map<String, Object> payload) {
        try {
            if (!payload.containsKey("month") || !payload.containsKey("year")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Month and year are required."));
            }

            int month = (Integer) payload.get("month");
            int year = (Integer) payload.get("year");

            if (month < 1 || month > 12) {
                return ResponseEntity.badRequest().body(Map.of("error", "Month must be between 1 and 12."));
            }

            if (year < 2000) {
                return ResponseEntity.badRequest().body(Map.of("error", "Year must be greater than 2000."));
            }

            List<Order> orders = orderService.getOrdersForExport(month, year);
            if (orders.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "no-orders"));
            }

            ByteArrayInputStream bis = excelService.createExcelReport(orders);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=orders.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(bis));

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid data type for month or year."));
        } catch (ExecutionException | InterruptedException | IOException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to export orders: " + e.getMessage()));
        }
    }

    @GetMapping("/shipments-list")
    public ResponseEntity<?> getShipmentsList() {
        try {
            List<ShipmentListDTO> shipments = orderService.getShipmentsForAdminList();
            
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(shipments);

            return ResponseEntity.ok(shipments);
            
        } catch (Throwable t) {
            t.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Errore interno del server durante il recupero delle spedizioni.", "message", t.getMessage()));
        }
    }

    // ... altri metodi del controller ...

     @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Object> updates) {
        try {
            Setting currentSettings = settingService.getSettings();

            // Aggiorna i campi solo se presenti nella richiesta
            if (updates.containsKey("standardShippingCost")) {
                currentSettings.setStandardShippingCost(new BigDecimal(updates.get("standardShippingCost").toString()));
            }
            if (updates.containsKey("freeShippingThreshold")) {
                currentSettings.setFreeShippingThreshold(new BigDecimal(updates.get("freeShippingThreshold").toString()));
            }
            if (updates.containsKey("splitShippingCost")) {
                currentSettings.setSplitShippingCost(new BigDecimal(updates.get("splitShippingCost").toString()));
            }

            settingService.saveSettings(currentSettings);
            return ResponseEntity.ok(Map.of("success", true, "message", "Settings updated successfully."));

        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Error updating settings: " + e.getMessage()));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid number format in settings."));
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<String> getVercelAnalytics(
            @RequestParam(defaultValue = "24h") String from,
            @RequestParam(defaultValue = "pageview") String type
    ) {
        try {
            String analyticsData = vercelAnalyticsService.getAnalyticsData(from, type);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json; charset=UTF-8");
            return new ResponseEntity<>(analyticsData, headers, HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("{\"error\": \"An internal error occurred while fetching analytics data.\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static class DeleteImageRequest {
        private String imageUrl;

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    @PostMapping("/images/delete")
    public ResponseEntity<?> deleteImage(@RequestBody DeleteImageRequest request) {
        if (request == null || request.getImageUrl() == null || request.getImageUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Image URL is required."));
        }
        try {
            cloudinaryService.deleteImage(request.getImageUrl());
            return ResponseEntity.ok(Map.of("success", true, "message", "Image deleted"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Error deleting image: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Collections.singletonMap("token", token));
        } else {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Credenziali non valide"));
        }
    }

    @PostMapping("/newsletter/subscribe")
    public ResponseEntity<?> adminSubscribe(@RequestBody NewsletterSubscriptionDTO subscription) {
        // Valida l'input
        if (subscription == null || subscription.getEmail() == null || subscription.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email is required."));
        }

        newsletterService.subscribe(subscription);
        // Il service gestisce già la logica dei duplicati, quindi possiamo semplicemente restituire OK.
        return ResponseEntity.ok(Map.of("success", true, "message", "Subscriber added or already exists."));
    }

    @PostMapping("/newsletter/unsubscribe")
    public ResponseEntity<?> adminUnsubscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email is required."));
        }

        try {
            newsletterService.unsubscribeByEmail(email);
            return ResponseEntity.ok(Map.of("success", true, "message", "Subscriber successfully unsubscribed."));
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error unsubscribing subscriber: " + e.getMessage()));
        }
    }

    @GetMapping("/newsletter/subscribers/count")
    public ResponseEntity<?> getSubscribersCount() {
        try {
            int count = newsletterService.countSubscribers();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving subscriber count: " + e.getMessage()));
        }
    }

    @PostMapping("/newsletter/send")
    public ResponseEntity<?> sendNewsletter(@RequestBody NewsletterRequestDTO newsletterRequest) {
        try {
            newsletterService.sendNewsletter(newsletterRequest.getSubject(), newsletterRequest.getMessage());
            return ResponseEntity.ok(Map.of("success", true, "message", "Newsletter inviata con successo."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Errore durante l'invio della newsletter."));
        }
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        try {
            DashboardStatsDTO stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }


    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(required = false) Integer status) {
        try {
            List<Order> orders = orderService.getParentOrders(status);
            return ResponseEntity.ok(orders);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable String id) {
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Ordine eliminato con successo"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
