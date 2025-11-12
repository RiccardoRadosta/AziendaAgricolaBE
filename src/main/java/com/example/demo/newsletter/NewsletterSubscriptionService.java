package com.example.demo.newsletter;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsletterSubscriptionService {

    private final NewsletterSubscriptionRepository newsletterSubscriptionRepository;

    public NewsletterSubscriptionService(NewsletterSubscriptionRepository newsletterSubscriptionRepository) {
        this.newsletterSubscriptionRepository = newsletterSubscriptionRepository;
    }

    public List<NewsletterSubscription> getSubscriptions() {
        return newsletterSubscriptionRepository.findAll();
    }

    public NewsletterSubscription createSubscription(NewsletterSubscription subscription) {
        return newsletterSubscriptionRepository.save(subscription);
    }
}
