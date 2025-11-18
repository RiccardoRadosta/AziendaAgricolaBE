package com.example.demo.admin;

import com.example.demo.admin.dto.DashboardStatsDTO;
import com.example.demo.order.Order;
import com.example.demo.order.OrderDTO;
import com.example.demo.order.OrderService;
import com.example.demo.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JwtUtil jwtUtil;
    private final OrderService orderService;
    private final DashboardService dashboardService; // Aggiunto il nuovo servizio

    public AdminController(JwtUtil jwtUtil, OrderService orderService, DashboardService dashboardService) {
        this.jwtUtil = jwtUtil;
        this.orderService = orderService;
        this.dashboardService = dashboardService; // Inizializzato nel costruttore
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if ("admin".equals(username) && "password".equals(password)) {
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Collections.singletonMap("token", token));
        } else {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Credenziali non valide"));
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
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(required = false) Integer status) throws ExecutionException, InterruptedException {
        List<Order> orders = orderService.getAllOrders(status);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, Integer> statusUpdate) {

        Integer newStatus = statusUpdate.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Campo 'status' mancante o non valido."));
        }

        try {
            Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/orders/{id}")
    public ResponseEntity<?> updateOrderDetails(
            @PathVariable String id,
            @RequestBody OrderDTO orderDetails) {
        try {
            Order updatedOrder = orderService.updateOrderDetails(id, orderDetails);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
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
