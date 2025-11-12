package com.example.demo.newsletter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class NewsletterSubscription {

    @Id
    @GeneratedValue
    private Long id;
    private String email;
    private boolean subscribed;

    public NewsletterSubscription() {
    }

    public NewsletterSubscription(String email, boolean subscribed) {
        this.email = email;
        this.subscribed = subscribed;
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

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }
}
