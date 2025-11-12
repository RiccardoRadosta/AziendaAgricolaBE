package com.example.demo.newsletter;

// Data Transfer Object: rappresenta i dati che arrivano dalla richiesta API
public class NewsletterSubscriptionDTO {
    private String email;

    // Getters e Setters necessari per la deserializzazione JSON
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
