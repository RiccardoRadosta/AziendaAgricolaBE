package com.example.demo.newsletter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Data Transfer Object: rappresenta i dati che arrivano dalla richiesta API
public class NewsletterSubscriptionDTO {

    // ===================== INIZIO DELLA MODIFICA =====================
    // @NotBlank: Assicura che l'email non sia nulla e non sia solo spazi bianchi.
    // @Email: Assicura che la stringa abbia il formato di un'email valida.
    @NotBlank(message = "L'indirizzo email non puo essere vuoto.")
    @Email(message = "Deve essere un indirizzo email valido.")
    // ===================== FINE DELLA MODIFICA =====================
    private String email;

    // Getters e Setters necessari per la deserializzazione JSON
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
