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
                    // Usa il token della carta (es. pm_...)
                    .setPaymentMethod(orderDTO.getPaymentToken())
                    // Lascia che Stripe gestisca il flusso di conferma.
                    // Se serve il 3D Secure, lo stato sarà 'requires_action'.
                    // Altrimenti, proverà a completare il pagamento direttamente.
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                    // OBBLIGATORIO: URL a cui Stripe può reindirizzare l'utente dopo l'autenticazione fuori sito
                    .setReturnUrl("http://localhost:3000/payment-confirmation") 
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Gestisci la risposta di Stripe
            if ("succeeded".equals(paymentIntent.getStatus())) {
                // CASO 1: Pagamento riuscito subito (es. carta senza 3DS)
                orderService.createOrder(orderDTO);
                Map<String, String> response = new HashMap<>();
                response.put("status", "succeeded");
                return ResponseEntity.ok(response);

            } else if ("requires_action".equals(paymentIntent.getStatus())) {
                // CASO 2: Richiesta autenticazione 3D Secure
                Map<String, String> response = new HashMap<>();
                response.put("status", "requires_action");
                response.put("clientSecret", paymentIntent.getClientSecret());
                // L'ordine NON viene salvato qui. Sarà salvato dal frontend dopo la conferma.
                return ResponseEntity.ok(response);

            } else {
                // Altri stati non gestiti (es. requires_payment_method, canceled)
                Map<String, String> response = new HashMap<>();
                response.put("error", "Stato del pagamento non valido: " + paymentIntent.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (StripeException e) {
            // CASO 3: Errore API da Stripe (es. carta rifiutata)
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
