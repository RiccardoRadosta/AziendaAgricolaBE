package com.example.demo.admin;

import com.example.demo.order.Order;
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

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    public AdminController(OrderService orderService, JwtUtil jwtUtil) {
        this.orderService = orderService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        // This is a simplified authentication check.
        // In a real-world application, you would validate credentials against a database.
        String username = credentials.get("username");
        String password = credentials.get("password");

        if ("admin".equals(username) && "password".equals(password)) {
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Collections.singletonMap("token", token));
        } else {
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/test")
    public String testAdmin() {
        return "Access to admin endpoint successful!";
    }

    @GetMapping("/orders")
    public List<Order> getOrders(@RequestParam(required = false) Integer status) throws ExecutionException, InterruptedException {
        return orderService.getAllOrders(status);
    }
}
