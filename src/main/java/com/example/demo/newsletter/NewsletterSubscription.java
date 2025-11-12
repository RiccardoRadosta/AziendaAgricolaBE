package com.example.demo.newsletter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.ZonedDateTime;

@Entity
public class NewsletterSubscription {

    @Id
    @GeneratedValue
    private Long id;
    private String email;
    private ZonedDateTime subscribedAt;

    public NewsletterSubscription() {
    }

    public NewsletterSubscription(String email, ZonedDateTime subscribedAt) {
        this.email = email;
        this.subscribedAt = subscribedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
