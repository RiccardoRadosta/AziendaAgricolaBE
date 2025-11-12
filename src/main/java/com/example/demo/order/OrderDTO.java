package com.example.demo.order;

// DTO che mappa esattamente i dati in ingresso dal frontend
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

    // Getters per tutti i campi (necessari per la deserializzazione JSON)

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getProvince() { return province; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public boolean isNewsletterSubscribed() { return newsletterSubscribed; }
    public String getOrderNotes() { return orderNotes; }
    public String getItems() { return items; }
    public double getSubtotal() { return subtotal; }
}
