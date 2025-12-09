package com.example.demo.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

@Service
public class VercelAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(VercelAnalyticsService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    // MODIFICA: Aggiunto valore di default vuoto (:) per rendere la proprietà opzionale all'avvio
    @Value("${vercel.api.token:}")
    private String vercelApiToken;

    // MODIFICA: Aggiunto valore di default vuoto (:) per rendere la proprietà opzionale all'avvio
    @Value("${vercel.project.id:}")
    private String vercelProjectId;

    @Value("${feature.vercel.analytics.enabled:false}")
    private boolean analyticsEnabled;

    public String getAnalyticsData(String fromParam, String typeParam) {
        if (!analyticsEnabled) {
            return "{\"status\": \"disabled\", \"message\": \"Vercel Analytics feature is not enabled.\"}";
        }

        if (vercelApiToken == null || vercelApiToken.isEmpty() || vercelProjectId == null || vercelProjectId.isEmpty()) {
            logger.error("Vercel Analytics feature is enabled, but API token or Project ID are missing from configuration.");
            return "{\"status\": \"error\", \"message\": \"Vercel configuration is missing.\"}";
        }
        
        logger.warn("Parameters 'from' and 'type' are deprecated. Using a fixed 24-hour window with the new Vercel v6 API.");

        Instant to = Instant.now();
        Instant from = to.minus(24, ChronoUnit.HOURS);
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        String fromTimestamp = formatter.format(from);
        String toTimestamp = formatter.format(to);

        String url = String.format(
            "https://api.vercel.com/v6/analytics/events?projectId=%s&from=%s&to=%s",
            vercelProjectId, fromTimestamp, toTimestamp
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + vercelApiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        logger.info("Sending request to Vercel API endpoint. URL: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            logger.info("Successfully received data from Vercel API v6.");
            return response.getBody();
        } catch (HttpClientErrorException e) {
            logger.error("Vercel API returned an error. Status: {}", e.getStatusCode());
            logger.error("Vercel API error response body: {}. Note: This API requires a Vercel Pro/Enterprise plan.", e.getResponseBodyAsString());
            return String.format("{\"status\": \"error\", \"message\": \"Failed to fetch Vercel Analytics. Reason: %s. This feature may require a paid Vercel plan.\"}", e.getStatusCode());
        }
    }
}
