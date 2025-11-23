package com.example.demo.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class BrevoEmailService {

    private final RestTemplate restTemplate;

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.api.url}")
    private String apiUrl;

    public BrevoEmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendOrderConfirmationEmail(Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", "Azienda Agricola");
        sender.put("email", "noreply@aziendaagricola.com");

        Map<String, Object> to = new HashMap<>();
        to.put("email", order.getEmail()); 
        to.put("name", order.getFullName());

        Map<String, Object> body = new HashMap<>();
        body.put("sender", sender);
        body.put("to", Collections.singletonList(to));
        body.put("subject", "Conferma d'ordine #" + order.getId());
        body.put("htmlContent", "<html><head></head><body><p>Ciao " + order.getFullName() + ",</p><p>Grazie per il tuo ordine! Il tuo ordine #" + order.getId() + " è stato confermato.</p></body></html>");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(apiUrl, request, String.class);
            System.out.println("Email di conferma inviata a " + order.getEmail());
        } catch (Exception e) {
            System.err.println("Errore durante l'invio dell'email: " + e.getMessage());
        }
    }
}
