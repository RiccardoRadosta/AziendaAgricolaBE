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
            // The frontend is responsible for confirming the PaymentIntent.
            // The backend's role is now to CREATE the intent with the correct parameters
            // and return the client_secret to the frontend.

            PaymentIntentCreateParams.PaymentMethodOptions.Card cardOptions = 
                PaymentIntentCreateParams.PaymentMethodOptions.Card.builder()
                    // This remains the crucial parameter to force the 3DS flow.
                    .setRequestThreeDSecure(PaymentIntentCreateParams.PaymentMethodOptions.Card.RequestThreeDSecure.ANY)
                    .build();

            PaymentIntentCreateParams.PaymentMethodOptions paymentMethodOptions = 
                PaymentIntentCreateParams.PaymentMethodOptions.builder()
                    .setCard(cardOptions)
                    .build();

            PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
                    .setAmount((long) (orderDTO.getSubtotal() * 100))
                    .setCurrency("eur")
                    // We do not set the payment method here anymore, the client will provide it during confirmation.
                    // .setPaymentMethod(orderDTO.getPaymentToken()) 
                    .setPaymentMethodOptions(paymentMethodOptions)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(createParams);

            // We simply return the client secret. The frontend will use this to call
            // stripe.confirmCardPayment(), which will handle the 3DS modal.
            Map<String, String> response = new HashMap<>();
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
