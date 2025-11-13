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
            // We are re-adopting the modern, single-call approach.
            // It's more robust for handling different authentication flows.
            PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
                    .setAmount((long) (orderDTO.getSubtotal() * 100))
                    .setCurrency("eur")
                    .setPaymentMethod(orderDTO.getPaymentToken())
                    // We tell Stripe to attempt confirmation immediately.
                    .setConfirm(true)
                    // CRUCIAL: We specify 'manual' confirmation.
                    // This tells Stripe that if authentication is required, we want to handle the
                    // next step on the client-side using the returned client_secret.
                    // This is the key to correctly triggering the 3D Secure flow without a 'card_declined' error.
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                    // The return_url is still mandatory for redirect-based authentications like 3D Secure.
                    .setReturnUrl("http://localhost:3000/payment-confirmation")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(createParams);

            // Handle the response from the single, unified API call
            if ("succeeded".equals(paymentIntent.getStatus())) {
                orderService.createOrder(orderDTO);
                Map<String, String> response = new HashMap<>();
                response.put("status", "succeeded");
                return ResponseEntity.ok(response);

            } else if ("requires_action".equals(paymentIntent.getStatus())) {
                // This is the flow we expect for the 3D Secure card.
                Map<String, String> response = new HashMap<>();
                response.put("status", "requires_action");
                response.put("clientSecret", paymentIntent.getClientSecret());
                return ResponseEntity.ok(response);

            } else {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Invalid payment intent status: " + paymentIntent.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (StripeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "An unknown error occurred with Stripe.");
            response.put("code", e.getCode());
            response.put("request-id", e.getRequestId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
