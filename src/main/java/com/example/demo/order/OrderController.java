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
                    // The frontend will confirm, so we only create the intent here.
                    .setPaymentMethodOptions(paymentMethodOptions)
                    // We also associate the payment method upon creation now
                    .setPaymentMethod(orderDTO.getPaymentToken())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(createParams);

            // The frontend expects both the clientSecret AND the status to decide its next action.
            // We restore the status field to the response.
            Map<String, String> response = new HashMap<>();
            response.put("status", "requires_action"); // This was the missing piece.
            response.put("clientSecret", paymentIntent.getClientSecret());
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "An unknown error occurred with Stripe.");
            response.put("code", e.getCode());
            response.put("request-id", e.getRequestId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
