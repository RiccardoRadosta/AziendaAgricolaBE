package com.example.demo.order;

import com.example.demo.product.InsufficientStockException;
import com.example.demo.product.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public OrderController(OrderService orderService, ProductService productService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/charge")
    public ResponseEntity<Map<String, String>> chargeOrder(@RequestBody OrderDTO orderDTO) {
        try {
            // 1. VERIFICA DISPONIBILITÀ STOCK (NUOVO CONTROLLO)
            List<Map<String, Object>> items = objectMapper.readValue(orderDTO.getItems(), new TypeReference<List<Map<String, Object>>>() {});
            productService.verifyStockAvailability(items);

            // 2. PROCEDI CON LA CREAZIONE DEL PAYMENTINTENT (Logica esistente)
            PaymentIntentCreateParams.PaymentMethodOptions.Card cardOptions = 
                PaymentIntentCreateParams.PaymentMethodOptions.Card.builder()
                    .setRequestThreeDSecure(PaymentIntentCreateParams.PaymentMethodOptions.Card.RequestThreeDSecure.ANY)
                    .build();

            PaymentIntentCreateParams.PaymentMethodOptions paymentMethodOptions = 
                PaymentIntentCreateParams.PaymentMethodOptions.builder()
                    .setCard(cardOptions)
                    .build();

            PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
                    .setAmount((long) (orderDTO.getSubtotal() * 100))
                    .setCurrency("eur")
                    .setPaymentMethodOptions(paymentMethodOptions)
                    .setPaymentMethod(orderDTO.getPaymentToken())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(createParams);

            Map<String, String> response = new HashMap<>();
            response.put("status", paymentIntent.getStatus());
            response.put("clientSecret", paymentIntent.getClientSecret());
            return ResponseEntity.ok(response);

        } catch (InsufficientStockException e) {
            // GESTIONE SPECIFICA DELLO STOCK ESAURITO
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response); // 409 Conflict
        
        } catch (StripeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "An unknown error occurred with Stripe.");
            response.put("code", e.getCode());
            response.put("request-id", e.getRequestId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (IOException | ExecutionException | InterruptedException e) {
            // GESTIONE GENERICA PER ALTRI ERRORI DURANTE LA VERIFICA
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to verify stock or process order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody OrderDTO orderDTO) {
        try {
            orderService.createOrder(orderDTO);
            Map<String, String> response = new HashMap<>();
            response.put("status", "order_created_successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to create order in database: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
