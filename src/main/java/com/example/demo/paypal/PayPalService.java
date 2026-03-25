package com.example.demo.paypal;

import com.example.demo.config.PayPalConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PayPalService {

    private static final Logger logger = LoggerFactory.getLogger(PayPalService.class);

    private final PayPalConfig payPalConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${frontend.url}")
    private String frontendBaseUrl;

    public PayPalService(PayPalConfig payPalConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.payPalConfig = payPalConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private String getAccessToken() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(payPalConfig.getClientId(), payPalConfig.getClientSecret());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                payPalConfig.getBaseUrl() + "/v1/oauth2/token",
                request,
                String.class
        );

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return responseJson.get("access_token").asText();
    }

    public String createOrder(double subtotal) throws IOException {
        String accessToken = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        String formattedSubtotal = new BigDecimal(subtotal).setScale(2, RoundingMode.HALF_UP).toString();

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("intent", "CAPTURE");

        ObjectNode purchaseUnit = objectMapper.createObjectNode();
        ObjectNode amount = objectMapper.createObjectNode();
        amount.put("currency_code", "EUR");
        amount.put("value", formattedSubtotal);
        purchaseUnit.set("amount", amount);

        requestBody.set("purchase_units", objectMapper.createArrayNode().add(purchaseUnit));

        // --- EXPERIENCE CONTEXT (SOLO DENTRO PAYMENT_SOURCE) ---
        String returnUrl = frontendBaseUrl + "/paypal/return";
        String cancelUrl = frontendBaseUrl + "/checkout?paypal=cancel";

        ObjectNode experienceContext = objectMapper.createObjectNode();
        experienceContext.put("return_url", returnUrl);
        experienceContext.put("cancel_url", cancelUrl);
        experienceContext.put("user_action", "PAY_NOW");
        experienceContext.put("shipping_preference", "NO_SHIPPING");
        experienceContext.put("brand_name", "Azienda Agricola");
        // Aggiungiamo un parametro aggiuntivo per forzare il pagamento immediato
        experienceContext.put("payment_method_preference", "IMMEDIATE_PAYMENT_REQUIRED");

        ObjectNode paymentSource = objectMapper.createObjectNode();
        ObjectNode paypal = objectMapper.createObjectNode();
        paypal.set("experience_context", experienceContext);
        paymentSource.set("paypal", paypal);
        requestBody.set("payment_source", paymentSource);
        // -------------------------------------------------------

        logger.info("Creating PayPal order. Amount: {}, Return: {}", formattedSubtotal, returnUrl);

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    payPalConfig.getBaseUrl() + "/v2/checkout/orders",
                    request,
                    String.class
            );

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String orderId = responseJson.get("id").asText();
            logger.info("PayPal order created: {}", orderId);
            return orderId;
        } catch (HttpClientErrorException e) {
            logger.error("PayPal Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            throw e;
        }
    }

    public String captureOrder(String orderId) throws IOException {
        String accessToken = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                payPalConfig.getBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture",
                request,
                String.class
        );

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return responseJson.get("status").asText();
    }

    public JsonNode getOrderDetails(String orderId) throws IOException {
        String accessToken = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                payPalConfig.getBaseUrl() + "/v2/checkout/orders/" + orderId,
                HttpMethod.GET,
                request,
                String.class
        );

        return objectMapper.readTree(response.getBody());
    }
}
