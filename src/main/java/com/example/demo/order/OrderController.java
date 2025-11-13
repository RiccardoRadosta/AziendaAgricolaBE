package com.example.demo.order;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/charge")
    public ResponseEntity<Map<String, String>> chargeOrder(@RequestBody OrderDTO orderDTO) {
        try {
            // Step 1: Create and confirm the PaymentIntent in a single call.
            // This is the correct way to handle flows that might require 3D Secure.
            PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
                    .setAmount((long) (orderDTO.getSubtotal() * 100))
                    .setCurrency("eur")
                    .setPaymentMethod(orderDTO.getPaymentToken()) // The PaymentMethod ID from the client
                    .setConfirm(true) // Tell Stripe to confirm the payment immediately
                    .setReturnUrl("http://localhost:3000/payment-confirmation") // Mandatory for 3D Secure
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(createParams);

            // Step 2: Handle the result of the confirmation.
            if ("succeeded".equals(paymentIntent.getStatus())) {
                orderService.createOrder(orderDTO);
                Map<String, String> response = new HashMap<>();
                response.put("status", "succeeded");
                return ResponseEntity.ok(response);

            } else if ("requires_action".equals(paymentIntent.getStatus())) {
                // This is the expected flow for the 3D Secure test card.
                Map<String, String> response = new HashMap<>();
                response.put("status", "requires_action");
                response.put("clientSecret", paymentIntent.getClientSecret());
                return ResponseEntity.ok(response);

            } else {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Stato del pagamento non valido: " + paymentIntent.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (StripeException e) {
            // A card decline error will still be caught here.
            // But with the correct params, the 3D Secure card should no longer be declined.
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "An unknown error occurred with Stripe.");
            response.put("code", e.getCode());
            response.put("request-id", e.getRequestId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
