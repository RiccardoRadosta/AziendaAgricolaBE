package com.example.demo.admin;

import com.example.demo.order.Order;
import com.example.demo.order.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;

    public AdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login() {
        // If Spring Security authentication is successful, this method will be called.
        return ResponseEntity.ok("Admin login successful");
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
