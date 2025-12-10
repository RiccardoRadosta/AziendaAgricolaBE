package com.example.demo.order;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
public class Order {
    @DocumentId
    private String id;

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
    private double subtotal; // Totale finale pagato
    private String shipmentPreference;
    private String status;
    private String siblingOrderId; // ID dell'ordine gemello per lo split

    // Riepilogo finanziario
    private double shippingCost;
    private double discount;
    private String couponCode;
    private double originalSubtotal; // Subtotale dei soli prodotti

    // Timestamp
    private Timestamp createdAt;
}
