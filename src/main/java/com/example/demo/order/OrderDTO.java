package com.example.demo.order;

import lombok.Data;

@Data
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
    private String shipmentPreference; // "single" or "split"

    // Campi finanziari aggiuntivi
    private double shippingCost;
    private double discount;
    private String couponCode;
    private double originalSubtotal;

    // Dati di pagamento
    private String paymentToken;
}
