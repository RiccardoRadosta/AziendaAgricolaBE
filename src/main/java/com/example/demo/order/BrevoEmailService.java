package com.example.demo.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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

  @Value("${frontend.url}")
  private String frontendUrl;

  public BrevoEmailService(
    RestTemplate restTemplate,
    ObjectMapper objectMapper,
    TemplateEngine templateEngine
  ) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.templateEngine = templateEngine;
  }

  public void sendInvoiceEmail(String toEmail, String orderId, MultipartFile attachment) {
      final Context ctx = new Context();
      ctx.setVariable("orderId", orderId);
      ctx.setVariable("currentYear", Year.now().getValue());

      final String htmlContent = this.templateEngine.process(
          "email/invoice-notification-template",
          ctx
      );

      String subject = "Fattura relativa al tuo ordine #" + orderId;
      sendEmailWithAttachment(toEmail, subject, htmlContent, attachment);
  }

  public void sendShippedOrderEmail(
    Order parentOrder,
    Order shipment
  ) {
    final Context ctx = new Context();

    ctx.setVariable("customerName", parentOrder.getFullName());
    ctx.setVariable("shipmentId", shipment.getId()); // ID della spedizione figlio
    ctx.setVariable("currentYear", Year.now().getValue());
    ctx.setVariable("termsUrl", frontendUrl + "/terms");


    Map<String, Object> shipmentForTemplate = new HashMap<>();
    shipmentForTemplate.put("trackingNumber", shipment.getTrackingNumber());
    try {
      List<Map<String, Object>> items = objectMapper.readValue(
        shipment.getItems(),
        new TypeReference<>() {}
      );
      shipmentForTemplate.put("items", items);
    } catch (IOException e) {
      shipmentForTemplate.put("items", Collections.emptyList()); // Gestione errore
    }
    ctx.setVariable("shipment", shipmentForTemplate);

    final String htmlContent = this.templateEngine.process(
        "email/shipped-order-template",
        ctx
      );

    // Oggetto email modificato per usare solo l'ID spedizione
    String subject = "La tua spedizione #" + shipment.getId() + " è in viaggio!";
    sendEmail(parentOrder.getEmail(), subject, htmlContent);
  }

  public void sendOrderConfirmationEmail(
    Order parentOrder,
    List<Order> childOrders
  ) {
    final Context ctx = new Context();

    // 1. Prepara i dati per il template
    ctx.setVariable("customerName", parentOrder.getFullName());
    ctx.setVariable("orderId", parentOrder.getId());
    ctx.setVariable("currentYear", Year.now().getValue());
    ctx.setVariable("termsUrl", frontendUrl + "/terms");


    // --- FIX: Gestione robusta dei valori numerici ---
    // Recupera i valori, usando 0.0 come fallback sicuro se sono null.
    double shippingCost = parentOrder.getShippingCost() != null
      ? parentOrder.getShippingCost()
      : 0.0;
    double discount = parentOrder.getDiscount() != null
      ? parentOrder.getDiscount()
      : 0.0;
    double total = parentOrder.getSubtotal() != null
      ? parentOrder.getSubtotal()
      : 0.0;

    // Il subtotale dei prodotti viene ricalcolato per coerenza,
    // partendo dal totale finale (che è già al netto dello sconto).
    double productSubtotal = total - shippingCost + discount;

    // Passa i valori come tipi numerici, non come stringhe.
    ctx.setVariable("subtotal", productSubtotal);
    ctx.setVariable("shippingCost", shippingCost);
    ctx.setVariable("discount", discount);
    ctx.setVariable("total", total);
    // --- FINE FIX ---

    // 2. Prepara la lista delle spedizioni in un formato leggibile dal template
    List<Map<String, Object>> shipmentsForTemplate = new ArrayList<>();
    for (Order shipment : childOrders) {
      Map<String, Object> shipmentMap = new HashMap<>();
      shipmentMap.put("status", getHumanReadableStatus(shipment.getStatus()));
      try {
        List<Map<String, Object>> items = objectMapper.readValue(
          shipment.getItems(),
          new TypeReference<>() {}
        );
        shipmentMap.put("items", items);
      } catch (IOException e) {
        shipmentMap.put("items", Collections.emptyList()); // Gestione errore
      }
      shipmentsForTemplate.add(shipmentMap);
    }
    ctx.setVariable("shipments", shipmentsForTemplate);

    // 3. Processa il template Thymeleaf per ottenere l'HTML
    final String htmlContent = this.templateEngine.process(
        "email/order-confirmation-template",
        ctx
      );

    // 4. Invia l'email con l'HTML generato
    String subject = "Conferma d'ordine #" + parentOrder.getId();
    sendEmail(parentOrder.getEmail(), subject, htmlContent);
  }

  public void sendEmail(String toEmail, String subject, String htmlContent) {
      sendEmailWithAttachment(toEmail, subject, htmlContent, null);
  }

  public void sendEmailWithAttachment(String toEmail, String subject, String htmlContent, MultipartFile attachment) {
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

    if (attachment != null && !attachment.isEmpty()) {
        try {
            Map<String, String> attachmentMap = new HashMap<>();
            attachmentMap.put("name", attachment.getOriginalFilename());
            attachmentMap.put("content", Base64.getEncoder().encodeToString(attachment.getBytes()));
            body.put("attachment", Collections.singletonList(attachmentMap));
        } catch (IOException e) {
            System.err.println("Errore durante la lettura dell'allegato: " + e.getMessage());
            // Potremmo decidere di lanciare un'eccezione o inviare senza allegato,
            // qui logghiamo e proseguiamo (o potremmo interrompere).
        }
    }

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<String> response = restTemplate.exchange(
        apiUrl,
        HttpMethod.POST,
        request,
        String.class
      );
      if (response.getStatusCode().is2xxSuccessful()) {
        System.out.println("Email inviata a " + toEmail + ".");
      } else {
        System.err.println(
          "Errore durante l'invio dell'email a " +
          toEmail +
          ". Stato: " +
          response.getStatusCode()
        );
      }
    } catch (HttpClientErrorException e) {
      System.err.println(
        "--- ERRORE HTTP INVIANDO EMAIL A " + toEmail + " ---"
      );
      System.err.println("Codice di stato: " + e.getStatusCode());
      System.err.println("MOTIVO: " + e.getResponseBodyAsString());
      System.err.println("-----------------------------------------");
    } catch (Exception e) {
      System.err.println(
        "Errore generico durante l'invio dell'email a " +
        toEmail +
        ": " +
        e.getMessage()
      );
    }
  }

  private String getHumanReadableStatus(String status) {
    if (status == null) return "Sconosciuto";
    switch (status) {
      case "0":
        return "In preparazione";
      case "1":
        return "Spedito";
      case "2":
        return "Consegnato";
      case "3":
        return "In Pre-ordine";
      case "4":
        return "Annullato";
      default:
        return status;
    }
  }
}
