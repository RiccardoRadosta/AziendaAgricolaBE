package com.example.demo.admin;

import com.example.demo.order.Order;
import com.example.demo.order.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;

    public AdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        // If this method is called, it means authentication was successful.
        // Spring Security's HttpBasic auth has already validated the credentials.
        return ResponseEntity.ok(Map.of("message", "Login successful"));
    }

    @GetMapping("/test")
    public String testAdmin() {
        return "Access to admin endpoint successful!";
    }

    @GetMapping("/orders")
    public List<Order> getOrders(@RequestParam(required = false) Integer status) throws ExecutionException, InterruptedException {
        return orderService.getAllOrders(status);
    }

    @PatchMapping("/orders/{orderId}")
    public Order updateOrderStatus(@PathVariable String orderId, @RequestBody Map<String, Integer> updates) throws ExecutionException, InterruptedException {
        Integer status = updates.get("status");
        if (status == null) {
            throw new IllegalArgumentException("Status field is required in the request body.");
        }
        return orderService.updateOrderStatus(orderId, status);
    }
}
