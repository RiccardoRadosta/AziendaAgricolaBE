package com.example.demo.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody OrderDTO orderDTO) {
        // Chiama il servizio per creare l'ordine
        orderService.createOrder(orderDTO);

        // Se il servizio non lancia eccezioni, l'ordine è stato creato.
        // Restituiamo una risposta semplice con uno stato HTTP 201 (Created).
        Map<String, String> response = Collections.singletonMap("status", "Order created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
