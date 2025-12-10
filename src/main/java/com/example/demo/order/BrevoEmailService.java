package com.example.demo.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.api.url}")
    private String apiUrl;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    public BrevoEmailService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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
                System.out.println("Email inviata a " + toEmail + ".");
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

    public void sendOrderConfirmationEmail(Order parentOrder, List<Order> childOrders) {
        String subject = "Conferma d'ordine #" + parentOrder.getId();
        String htmlContent = buildConfirmationHtml(parentOrder, childOrders);
        sendEmail(parentOrder.getEmail(), subject, htmlContent);
    }

    private String buildConfirmationHtml(Order parent, List<Order> children) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h1>Grazie per il tuo acquisto, ").append(parent.getFullName()).append("!</h1>");
        html.append("<p>Il tuo ordine <strong>#").append(parent.getId()).append("</strong> è stato confermato e verrà elaborato a breve.</p>");

        for (int i = 0; i < children.size(); i++) {
            Order shipment = children.get(i);
            html.append("<h2>Spedizione ").append(i + 1).append(" di ").append(children.size()).append("</h2>");
            html.append("<p>Stato: <strong>").append(getHumanReadableStatus(shipment.getStatus())).append("</strong></p>");
            html.append("<ul>");

            try {
                List<Map<String, Object>> items = objectMapper.readValue(shipment.getItems(), new TypeReference<>() {});
                for (Map<String, Object> item : items) {
                    html.append("<li>").append(item.get("name")).append(" - Quantità: ").append(item.get("quantity")).append("</li>");
                }
            } catch (IOException e) {
                html.append("<li>Errore nel leggere gli articoli della spedizione.</li>");
            }

            html.append("</ul>");
        }

        html.append("<h2>Riepilogo finanziario</h2>");
        html.append("<p>Subtotale prodotti: ").append(String.format("%.2f", parent.getSubtotal() - parent.getShippingCost() + parent.getDiscount())).append(" €</p>");
        html.append("<p>Costo spedizione: ").append(String.format("%.2f", parent.getShippingCost())).append(" €</p>");
        if (parent.getDiscount() > 0) {
            html.append("<p>Sconto applicato: - ").append(String.format("%.2f", parent.getDiscount())).append(" €</p>");
        }
        html.append("<h3>Totale pagato: ").append(String.format("%.2f", parent.getSubtotal())).append(" €</h3>");

        html.append("</body></html>");
        return html.toString();
    }

    private String getHumanReadableStatus(String status) {
        if (status == null) return "Sconosciuto";
        switch (status) {
            case "0": return "In preparazione";
            case "1": return "Spedito";
            case "2": return "Annullato";
            case "3": return "In Pre-ordine";
            default: return status;
        }
    }
}
