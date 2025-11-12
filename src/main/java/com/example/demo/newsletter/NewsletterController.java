
package com.example.demo.newsletter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;
    private static final Logger logger = LoggerFactory.getLogger(NewsletterController.class);

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody NewsletterSubscriptionDTO subscriptionDTO) {
        logger.info("Received subscription request for email: {}", subscriptionDTO.getEmail());
        newsletterService.subscribe(subscriptionDTO);
        return ResponseEntity.ok().build(); 
    }
}
