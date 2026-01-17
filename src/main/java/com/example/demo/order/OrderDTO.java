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

    // Campi fatturazione (opzionali)
    private Integer richiestaFattura; // 1 se richiesta, 0 o null altrimenti
    private String indirizzoFatturazione;
    private String capFatturazione;
    private String cittaFatturazione;
    private String provinciaFatturazione;
    private String nazioneFatturazione;
    private String tipoFatturazione; // "persona_fisica" o "azienda"

    // Dati per persona fisica
    private String nomeFatturazione;
    private String cognomeFatturazione;
    private String codiceFiscaleFatturazione;

    // Dati per azienda
    private String ragioneSocialeFatturazione;
    private String partitaIvaFatturazione;
    private String codiceUnivocoSdiFatturazione;
}
