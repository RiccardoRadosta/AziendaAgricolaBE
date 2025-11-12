package com.example.demo.newsletter;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @PostMapping("/subscribe") // Endpoint più descrittivo
    public NewsletterSubscription subscribe(@RequestBody NewsletterSubscriptionDTO subscriptionDTO) {
        return newsletterService.subscribe(subscriptionDTO);
    }
}
