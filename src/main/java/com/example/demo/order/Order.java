package com.example.demo.order;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.Exclude;
import lombok.Data;

import java.util.List;

@Data
public class Order {
    @DocumentId
    private String id;

    // -- Campi Gerarchici --
    private String type; // "PARENT" o "CHILD"
    private String parentOrderId;
    private List<String> childOrderIds;

    @Exclude // Questo campo non verrà salvato in Firestore
    private List<Order> childOrders;

    // -- Dati del Cliente (solo per PARENT) --
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

    // -- Campi per la ricerca --
    private List<String> searchKeywords;
    private String email_lowercase;

    // -- Dati Finanziari (solo per PARENT) --
    private Double subtotal; // Totale finale pagato
    private Double shippingCost;
    private Double discount;
    private String couponCode;

    // -- Dati della Spedizione (solo per CHILD) --
    private String items; // JSON degli articoli in questa spedizione
    private Double originalSubtotal; // Valore dei soli articoli in questa spedizione
    private String trackingNumber; // NUOVO CAMPO

    // -- Dati Comuni --
    private String status;
    private Timestamp createdAt;
    private String shipmentPreference; // Mantenuto per riferimento sul parent
}
