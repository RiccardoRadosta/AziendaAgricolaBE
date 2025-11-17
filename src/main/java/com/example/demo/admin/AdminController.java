package com.example.demo.admin;

import com.example.demo.order.Order;
import com.example.demo.order.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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
        // This is a temporary fix to align with the frontend's expectations.
        // It now returns a JSON object.
        // TODO: Implement proper authentication and JWT generation.
        return ResponseEntity.ok(Collections.singletonMap("message", "Admin login successful"));
    }

    @GetMapping("/test")
    public String testAdmin() {
        return "Access to admin endpoint successful!";
    }

    @GetMapping("/orders")
    public List<Order> getOrders(@RequestParam(required = false) Integer status) throws ExecutionException, InterruptedException {
        // Reverted to the original and correct logic
        return orderService.getAllOrders(status);
    }
}
