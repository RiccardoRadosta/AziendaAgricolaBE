package com.example.demo.order;

import lombok.Data;

@Data
public class OrderDTO {
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

    // Dati di fatturazione
    private Integer richiestaFattura;
    private String indirizzoFatturazione;
    private String capFatturazione;
    private String cittaFatturazione;
    private String provinciaFatturazione;
    private String nazioneFatturazione;
    private String tipoFatturazione;
    private String nomeFatturazione;
    private String cognomeFatturazione;
    private String codiceFiscaleFatturazione;
    private String ragioneSocialeFatturazione;
    private String partitaIvaFatturazione;
    private String codiceUnivocoSdiFatturazione;

    private Double subtotal;
    private Double shippingCost;
    private Double discount;
    private String couponCode;
    private String paymentToken; // Token di Stripe (opzionale se si usa PayPal)
    private String paymentMethod; // NUOVO CAMPO

    private String items; // JSON string
    private String shipmentPreference;
}
