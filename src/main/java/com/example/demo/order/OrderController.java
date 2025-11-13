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

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody OrderDTO orderDTO) {
        orderService.createOrder(orderDTO);
        Map<String, String> response = new HashMap<>();
        response.put("status", "Order created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/charge")
    public ResponseEntity<Map<String, String>> chargeOrder(@RequestBody OrderDTO orderDTO) {
        try {
            // 1. Create parameters for the charge on Stripe
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((long) (orderDTO.getSubtotal() * 100))
                    .setCurrency("eur")
                    .setPaymentMethod(orderDTO.getPaymentToken())
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                    .setConfirm(true)
                    // ADDED: Stripe requires a return_url for redirect-based payment methods
                    .setReturnUrl("http://localhost:3000/payment-confirmation")
                    .build();

            // 2. Create and confirm the PaymentIntent
            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // 3. Handle the response from Stripe
            if ("succeeded".equals(paymentIntent.getStatus())) {
                // CASE 1: Payment is successful immediately
                orderService.createOrder(orderDTO);
                Map<String, String> response = new HashMap<>();
                response.put("status", "succeeded");
                return ResponseEntity.ok(response);

            } else if ("requires_action".equals(paymentIntent.getStatus())) {
                // CASE 2: Payment requires 3D Secure authentication
                Map<String, String> response = new HashMap<>();
                response.put("status", "requires_action");
                response.put("clientSecret", paymentIntent.getClientSecret());
                return ResponseEntity.ok(response); // Return 200 OK for the frontend to handle

            } else {
                 // Other statuses (e.g., requires_payment_method, canceled)
                Map<String, String> response = new HashMap<>();
                response.put("error", "Invalid payment intent status");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (StripeException e) {
            // CASE 3: An error occurred (e.g., card declined)
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
