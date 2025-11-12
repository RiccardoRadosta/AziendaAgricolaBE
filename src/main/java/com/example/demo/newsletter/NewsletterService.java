
package com.example.demo.newsletter;

import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class NewsletterService {

    private final Firestore firestore;
    private static final Logger logger = LoggerFactory.getLogger(NewsletterService.class);

    public NewsletterService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void subscribe(NewsletterSubscriptionDTO subscriptionDTO) {
        logger.info("Processing subscription for email: {}", subscriptionDTO.getEmail());
        NewsletterSubscription subscription = new NewsletterSubscription();
        subscription.setEmail(subscriptionDTO.getEmail());
        subscription.setSubscribedAt(ZonedDateTime.now());
        subscription.setId(UUID.randomUUID().toString());

        firestore.collection("newsletterSubscriptions").document(subscription.getId()).set(subscription);
        logger.info("Subscription saved to Firestore with ID: {}", subscription.getId());
    }
}
