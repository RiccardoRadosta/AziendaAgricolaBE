package com.example.demo.paypal;

import com.example.demo.order.OrderDTO;
import com.example.demo.order.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/paypal") // <-- MODIFICATO
public class PayPalController {

    private final PayPalService payPalService;
    private final OrderService orderService;

    public PayPalController(PayPalService payPalService, OrderService orderService) {
        this.payPalService = payPalService;
        this.orderService = orderService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody OrderDTO orderDTO) {
        try {
            // --- VERIFICA DI SICUREZZA SUL TOTALE ---
            double serverTotal = orderService.calculateOrderTotal(orderDTO);
            double clientTotal = orderDTO.getSubtotal();

            if (Math.abs(serverTotal - clientTotal) > 0.01) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Order total mismatch. Client: " + clientTotal + ", Server: " + serverTotal);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            // --- FINE VERIFICA ---

            String orderId = payPalService.createOrder(serverTotal); // Usa il totale calcolato dal server
            Map<String, String> response = new HashMap<>();
            response.put("orderId", orderId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/capture-order")
    public ResponseEntity<?> captureOrder(@RequestBody Map<String, String> payload) {
        try {
            String orderId = payload.get("orderId");
            String status = payPalService.captureOrder(orderId);

            Map<String, String> response = new HashMap<>();
            response.put("status", status);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
