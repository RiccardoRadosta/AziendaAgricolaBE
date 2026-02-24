package com.example.demo.payment;

import com.example.demo.order.OrderDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/klarna")
public class KlarnaController {

    private final KlarnaService klarnaService;

    public KlarnaController(KlarnaService klarnaService) {
        this.klarnaService = klarnaService;
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody OrderDTO orderDTO) {
        try {
            String checkoutUrl = klarnaService.createCheckoutSession(orderDTO);
            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating Klarna session: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-order")
    public ResponseEntity<?> verifyOrder(@RequestBody Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        if (sessionId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId is required"));
        }

        try {
            klarnaService.verifyAndCreateOrder(sessionId);
            return ResponseEntity.ok(Map.of("status", "order_created"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error verifying order: " + e.getMessage()));
        }
    }
}
