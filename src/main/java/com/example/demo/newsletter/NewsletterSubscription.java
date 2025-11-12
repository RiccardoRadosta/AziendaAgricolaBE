package com.example.demo.newsletter;

import java.time.ZonedDateTime;

public class NewsletterSubscription {

    private String id; // Changed from Long to String for Firestore
    private String email;
    private ZonedDateTime subscribedAt;

    public NewsletterSubscription() {
    }

    public NewsletterSubscription(String email, ZonedDateTime subscribedAt) {
        this.email = email;
        this.subscribedAt = subscribedAt;
    }

    public String getId() { // Changed from Long to String
        return id;
    }

    public void setId(String id) { // Changed from Long to String
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ZonedDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(ZonedDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }
}
