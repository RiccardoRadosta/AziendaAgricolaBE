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

    @Value("${vercel.api.token}")
    private String vercelApiToken;

    @Value("${vercel.project.id}")
    private String vercelProjectId;

    // Metodo aggiornato per usare l'endpoint corretto V6 che hai trovato.
    public String getAnalyticsData(String fromParam, String typeParam) {
        logger.warn("Parameters 'from' and 'type' are deprecated. Using a fixed 24-hour window with the new Vercel v6 API.");

        // Genera i timestamp nel formato corretto richiesto dall'API v6
        Instant to = Instant.now();
        Instant from = to.minus(24, ChronoUnit.HOURS);
        
        // Formattatore per lo standard ISO 8601 richiesto da Vercel (es: 2023-01-01T00:00:00.000Z)
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        String fromTimestamp = formatter.format(from);
        String toTimestamp = formatter.format(to);

        // Costruisce l'URL corretto con l'endpoint V6
        String url = String.format(
            "https://api.vercel.com/v6/analytics/events?projectId=%s&from=%s&to=%s",
            vercelProjectId, fromTimestamp, toTimestamp
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + vercelApiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        logger.info("Sending request to correct Vercel API endpoint. URL: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            logger.info("Successfully received data from Vercel API v6.");
            return response.getBody();
        } catch (HttpClientErrorException e) {
            logger.error("Vercel API returned an error. Status: {}", e.getStatusCode());
            logger.error("Vercel API error response body: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
}
