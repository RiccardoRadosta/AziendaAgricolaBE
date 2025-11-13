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

    @PostMapping("/charge")
    public ResponseEntity<Map<String, String>> chargeOrder(@RequestBody OrderDTO orderDTO) {
        try {
            // 1. Creare i parametri per l'addebito su Stripe
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    // Stripe usa la più piccola unità di valuta (centesimi per EUR)
                    .setAmount((long) (orderDTO.getSubtotal() * 100))
                    .setCurrency("eur")
                    // Il "payment_method" corrisponde al token generato da Stripe.js nel frontend
                    .setPaymentMethod(orderDTO.getPaymentToken())
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                    .setConfirm(true)
                    .build();

            // 2. Creare e confermare l'addebito (PaymentIntent)
            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // 3. Se l'addebito ha successo, procedere con la creazione dell'ordine
            if ("succeeded".equals(paymentIntent.getStatus())) {
                orderService.createOrder(orderDTO);
                Map<String, String> response = Collections.singletonMap("status", "Payment successful and order created");
                return ResponseEntity.ok(response);
            } else {
                // L'addebito non è andato a buon fine per qualche motivo (es. richiesto 3D Secure)
                Map<String, String> response = Collections.singletonMap("status", "Payment requires action");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (StripeException e) {
            // 4. Se c'è un errore da Stripe (es. carta rifiutata), ritorna un errore
            Map<String, String> response = Collections.singletonMap("status", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
