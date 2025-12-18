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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.api.url}")
    private String apiUrl;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    public BrevoEmailService(RestTemplate restTemplate, ObjectMapper objectMapper, TemplateEngine templateEngine) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.templateEngine = templateEngine;
    }

    public void sendOrderConfirmationEmail(Order parentOrder, List<Order> childOrders) {
        final Context ctx = new Context();

        // 1. Prepara i dati per il template
        ctx.setVariable("customerName", parentOrder.getFullName());
        ctx.setVariable("orderId", parentOrder.getId());
        ctx.setVariable("currentYear", Year.now().getValue());

        // Riepilogo finanziario
        double productSubtotal = parentOrder.getSubtotal() - parentOrder.getShippingCost() + parentOrder.getDiscount();
        ctx.setVariable("subtotal", String.format("%.2f", productSubtotal));
        ctx.setVariable("shippingCost", String.format("%.2f", parentOrder.getShippingCost()));
        ctx.setVariable("discount", String.format("%.2f", parentOrder.getDiscount()));
        ctx.setVariable("total", String.format("%.2f", parentOrder.getSubtotal()));

        // 2. Prepara la lista delle spedizioni in un formato leggibile dal template
        List<Map<String, Object>> shipmentsForTemplate = new ArrayList<>();
        for (Order shipment : childOrders) {
            Map<String, Object> shipmentMap = new HashMap<>();
            shipmentMap.put("status", getHumanReadableStatus(shipment.getStatus()));
            try {
                List<Map<String, Object>> items = objectMapper.readValue(shipment.getItems(), new TypeReference<>() {});
                shipmentMap.put("items", items);
            } catch (IOException e) {
                shipmentMap.put("items", Collections.emptyList()); // Gestione errore
            }
            shipmentsForTemplate.add(shipmentMap);
        }
        ctx.setVariable("shipments", shipmentsForTemplate);

        // 3. Processa il template Thymeleaf per ottenere l'HTML
        final String htmlContent = this.templateEngine.process("email/order-confirmation-template", ctx);

        // 4. Invia l'email con l'HTML generato
        String subject = "Conferma d'ordine #" + parentOrder.getId();
        sendEmail(parentOrder.getEmail(), subject, htmlContent);
    }

    // === FIX: Riportato il metodo a public per renderlo accessibile da altri servizi ===
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
