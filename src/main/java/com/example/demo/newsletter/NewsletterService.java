package com.example.demo.newsletter;

import com.example.demo.order.BrevoEmailService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class NewsletterService {

    private final Firestore firestore;
    private final BrevoEmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(NewsletterService.class);

    public NewsletterService(Firestore firestore, BrevoEmailService emailService) {
        this.firestore = firestore;
        this.emailService = emailService;
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

    public void sendNewsletter(String subject, String message) throws ExecutionException, InterruptedException {
        logger.info("Starting newsletter send job...");
        ApiFuture<QuerySnapshot> future = firestore.collection("newsletterSubscriptions").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (documents.isEmpty()) {
            logger.warn("No subscribers found. Newsletter job finished.");
            return;
        }

        logger.info("Found {} subscribers. Proceeding to send emails.", documents.size());

        for (QueryDocumentSnapshot document : documents) {
            String email = document.getString("email");
            if (email != null && !email.isEmpty()) {
                try {
                    emailService.sendEmail(email, subject, message);
                    logger.info("Successfully sent newsletter to: {}", email);
                } catch (Exception e) {
                    logger.error("Failed to send newsletter to: {}. Error: {}", email, e.getMessage());
                }
            }
        }
        logger.info("Newsletter send job finished.");
    }
}
