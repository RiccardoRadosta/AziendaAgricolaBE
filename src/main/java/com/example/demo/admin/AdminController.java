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
        // As per the user's request, we now return a fake token to align with the frontend's expectations.
        // This allows testing the full login flow before implementing real JWT logic.
        return ResponseEntity.ok(Collections.singletonMap("token", "fake-jwt-token-for-testing"));
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
