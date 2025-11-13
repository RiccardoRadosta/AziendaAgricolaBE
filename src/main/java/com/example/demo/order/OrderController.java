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
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((long) (orderDTO.getSubtotal() * 100))
                    .setCurrency("eur")
                    .setPaymentMethod(orderDTO.getPaymentToken())
                    // Usa la conferma automatica per gestire il 3D Secure
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                    // Conferma il pagamento immediatamente
                    .setConfirm(true)
                    // OBBLIGATORIO: Fornisci un return_url come richiesto da Stripe per i flussi di pagamento automatici
                    .setReturnUrl("http://localhost:3000/payment-confirmation")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            if ("succeeded".equals(paymentIntent.getStatus())) {
                orderService.createOrder(orderDTO);
                Map<String, String> response = new HashMap<>();
                response.put("status", "succeeded");
                return ResponseEntity.ok(response);

            } else if ("requires_action".equals(paymentIntent.getStatus())) {
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
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
