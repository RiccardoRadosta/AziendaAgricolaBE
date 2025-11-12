package com.example.demo.newsletter;

import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;

@Service
public class NewsletterService {

    private final NewsletterRepository newsletterRepository;

    public NewsletterService(NewsletterRepository newsletterRepository) {
        this.newsletterRepository = newsletterRepository;
    }

    public NewsletterSubscription subscribe(NewsletterSubscriptionDTO subscriptionDTO) {
        NewsletterSubscription subscriptionEntity = new NewsletterSubscription();
        subscriptionEntity.setEmail(subscriptionDTO.getEmail());
        subscriptionEntity.setSubscribedAt(ZonedDateTime.now());
        return newsletterRepository.save(subscriptionEntity);
    }
}
