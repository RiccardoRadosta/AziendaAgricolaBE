package com.example.demo.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class VercelAnalyticsService {

    @Value("${vercel.api.token}")
    private String vercelApiToken;

    @Value("${vercel.project.id}")
    private String vercelProjectId;

    private final RestTemplate restTemplate;

    public VercelAnalyticsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches analytics data from the Vercel API.
     * @param from The start of the time range (e.g., "24h", "7d", or a timestamp).
     * @param type The type of event to fetch (e.g., "pageview", "visitor").
     * @return A JSON string containing the analytics data from Vercel.
     */
    public String getAnalyticsData(String from, String type) {
        // Build the URL with query parameters
        String url = UriComponentsBuilder.fromHttpUrl("https://api.vercel.com/v1/analytics/events")
                .queryParam("projectId", vercelProjectId)
                .queryParam("from", from)
                .queryParam("type", type)
                .toUriString();

        // Set up the authorization header
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(vercelApiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Make the REST call
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }
}
