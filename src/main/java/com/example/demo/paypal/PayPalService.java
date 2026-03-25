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
import java.util.HashMap;
import java.util.Map;

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

    public Map<String, String> createOrder(double subtotal) throws IOException {
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

        // Sanificazione URL
        String cleanBaseUrl = frontendBaseUrl.replaceAll("^['\"]|['\"]$", "");
        String returnUrl = cleanBaseUrl + "/paypal/return";
        String cancelUrl = cleanBaseUrl + "/checkout?paypal=cancel";

        // Usiamo application_context a livello radice: è il modo più affidabile per ottenere l'approveUrl per il redirect
        ObjectNode appContext = objectMapper.createObjectNode();
        appContext.put("return_url", returnUrl);
        appContext.put("cancel_url", cancelUrl);
        appContext.put("user_action", "PAY_NOW");
        appContext.put("shipping_preference", "NO_SHIPPING");
        appContext.put("brand_name", "Azienda Agricola");
        
        requestBody.set("application_context", appContext);

        logger.info("PayPal Create Order: amount={}, return={}", formattedSubtotal, returnUrl);

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    payPalConfig.getBaseUrl() + "/v2/checkout/orders",
                    request,
                    String.class
            );

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String orderId = responseJson.get("id").asText();
            
            String approveUrl = "";
            if (responseJson.has("links")) {
                for (JsonNode link : responseJson.get("links")) {
                    String rel = link.get("rel").asText();
                    if ("approve".equals(rel) || "payer-action".equals(rel)) {
                        approveUrl = link.get("href").asText();
                        break;
                    }
                }
            }

            // Fallback: se non trova 'approve', cerca il link 'checkoutnow' (comune in alcuni flussi)
            if (approveUrl.isEmpty() && responseJson.has("links")) {
                for (JsonNode link : responseJson.get("links")) {
                    if (link.get("href").asText().contains("checkoutnow")) {
                        approveUrl = link.get("href").asText();
                        break;
                    }
                }
            }

            logger.info("PayPal Order Created: ID={}, approveUrl={}", orderId, approveUrl);
            
            Map<String, String> result = new HashMap<>();
            result.put("orderId", orderId);
            result.put("approveUrl", approveUrl);
            return result;
            
        } catch (HttpClientErrorException e) {
            logger.error("PayPal Create Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            logger.error("PayPal Create Unexpected Error: {}", e.getMessage());
            throw e;
        }
    }

    public String captureOrder(String orderId) throws IOException {
        String accessToken = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(null, headers);

        try {
            logger.info("PayPal Capture attempting for orderId: {}", orderId);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    payPalConfig.getBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture",
                    request,
                    String.class
            );

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String status = responseJson.get("status").asText();
            logger.info("PayPal Capture successful: {} for orderId: {}", status, orderId);
            return status;
        } catch (HttpClientErrorException e) {
            logger.error("PayPal Capture Error for ID {}: {} - {}", orderId, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            logger.error("PayPal Capture Unexpected Error for ID {}: {}", orderId, e.getMessage());
            throw e;
        }
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
