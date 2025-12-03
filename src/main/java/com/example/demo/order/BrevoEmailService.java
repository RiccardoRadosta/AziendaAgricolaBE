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

    @Value("${brevo.sender.email}")
    private String senderEmail;

    public BrevoEmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendEmail(String toEmail, String subject, String htmlContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", "Azienda Agricola");
        sender.put("email", senderEmail);

        Map<String, Object> to = new HashMap<>();
        to.put("email", toEmail);

        Map<String, Object> body = new HashMap<>();
        body.put("sender", sender);
        body.put("to", Collections.singletonList(to));
        body.put("subject", subject);
        body.put("htmlContent", htmlContent);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Email inviata a " + toEmail + ". Risposta del server: " + response.getBody());
            } else {
                System.err.println("Errore durante l'invio dell'email a " + toEmail + ". Stato: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            System.err.println("--- ERRORE HTTP INVIANDO EMAIL A " + toEmail + " ---");
            System.err.println("Codice di stato: " + e.getStatusCode());
            System.err.println("MOTIVO: " + e.getResponseBodyAsString());
            System.err.println("-----------------------------------------");
        } catch (Exception e) {
            System.err.println("Errore generico durante l'invio dell'email a " + toEmail + ": " + e.getMessage());
        }
    }

    public void sendOrderConfirmationEmail(Order order) {
        String subject = "Conferma d'ordine #" + order.getId();
        String htmlContent = "<html><head></head><body><p>Ciao " + order.getFullName() + ",</p><p>Grazie per il tuo ordine! Il tuo ordine #" + order.getId() + " è stato confermato.</p></body></html>";
        sendEmail(order.getEmail(), subject, htmlContent);
    }
}
