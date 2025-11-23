package com.example.demo.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Richiesta a Brevo accettata. Risposta del server: " + response.getBody());
            } else {
                System.err.println("Richiesta a Brevo completata ma con stato di errore: " + response.getStatusCode());
                System.err.println("Corpo della risposta: " + response.getBody());
            }
        } catch (HttpClientErrorException e) {
            System.err.println("--- ERRORE DURANTE L'INVIO DELL'EMAIL ---");
            System.err.println("Codice di stato HTTP: " + e.getStatusCode());
            System.err.println("MOTIVO DELL'ERRORE (da Brevo): " + e.getResponseBodyAsString());
            System.err.println("-----------------------------------------");
        } catch (Exception e) {
            System.err.println("Errore generico non previsto durante l'invio dell'email: " + e.getMessage());
        }
    }
}
