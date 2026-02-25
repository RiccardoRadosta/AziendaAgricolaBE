package com.example.demo.newsletter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;
    private static final Logger logger = LoggerFactory.getLogger(NewsletterController.class);

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@Valid @RequestBody NewsletterSubscriptionDTO subscriptionDTO) {
        // logger.info("Received subscription request for email: {}", subscriptionDTO.getEmail()); // RIMOSSO
        
        boolean isNewSubscription = newsletterService.subscribe(subscriptionDTO);
        
        if (!isNewSubscription) {
            return ResponseEntity.ok(Map.of(
                "errorCode", "NEWSLETTER_ALREADY_SUBSCRIBED",
                "message", "Email già registrata"
            ));
        }

        return ResponseEntity.ok().build(); 
    }
}
