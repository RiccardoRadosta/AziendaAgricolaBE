package com.example.demo.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VercelAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(VercelAnalyticsService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${vercel.api.token}")
    private String vercelApiToken;

    @Value("${vercel.project.id}")
    private String vercelProjectId;

    public String getAnalyticsData(String from, String type) {
        // CORREZIONE: Il projectId deve essere un parametro della query, non parte del path.
        String url = String.format(
            "https://api.vercel.com/v1/analytics/data?projectId=%s&from=%s&type=%s",
            vercelProjectId, from, type
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + vercelApiToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        logger.info("Sending request to Vercel API.");
        logger.info("URL: {}", url);
        logger.info("Headers: {}", headers);

        // Eseguiamo la chiamata e restituiamo la risposta
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }
}
