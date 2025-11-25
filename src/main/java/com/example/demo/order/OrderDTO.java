package com.example.demo.order;

import lombok.Getter;
import lombok.Setter;

// DTO che mappa esattamente i dati in ingresso dal frontend
@Getter
@Setter
public class OrderDTO {

    // Dati del cliente
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private boolean newsletterSubscribed;
    private String orderNotes;

    // Dati dell'ordine
    private String items; // Es. JSON o stringa formattata con i prodotti
    private double subtotal;

    // Dati di pagamento
    private String paymentToken;
}
