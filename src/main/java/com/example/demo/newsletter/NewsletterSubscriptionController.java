package com.example.demo.newsletter;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterSubscriptionController {

    private final NewsletterSubscriptionService newsletterSubscriptionService;

    public NewsletterSubscriptionController(NewsletterSubscriptionService newsletterSubscriptionService) {
        this.newsletterSubscriptionService = newsletterSubscriptionService;
    }

    @GetMapping
    public List<NewsletterSubscription> getSubscriptions() {
        return newsletterSubscriptionService.getSubscriptions();
    }

    @PostMapping
    public NewsletterSubscription createSubscription(@RequestBody NewsletterSubscription subscription) {
        return newsletterSubscriptionService.createSubscription(subscription);
    }
}
