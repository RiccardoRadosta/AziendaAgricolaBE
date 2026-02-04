package com.example.demo.paypal;

import com.example.demo.config.PayPalConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PayPalService {

    private final PayPalConfig payPalConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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

        // Formatta il subtotal a due cifre decimali, come richiesto da PayPal
        String formattedSubtotal = new BigDecimal(subtotal).setScale(2, RoundingMode.HALF_UP).toString();

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("intent", "CAPTURE");

        ObjectNode purchaseUnit = objectMapper.createObjectNode();
        ObjectNode amount = objectMapper.createObjectNode();
        amount.put("currency_code", "EUR");
        amount.put("value", formattedSubtotal);
        purchaseUnit.set("amount", amount);

        requestBody.set("purchase_units", objectMapper.createArrayNode().add(purchaseUnit));

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                payPalConfig.getBaseUrl() + "/v2/checkout/orders",
                request,
                String.class
        );

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return responseJson.get("id").asText();
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
}
