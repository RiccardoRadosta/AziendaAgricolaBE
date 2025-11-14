package com.example.demo.order;

import java.util.List;

// DTO che mappa la richiesta in arrivo dal frontend
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
    private List<Object> items; // Modificato per accettare un vero array di oggetti JSON
    private double subtotal;

    // Dati di pagamento
    private String paymentToken;

    // Getters
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
    public List<Object> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
    public String getPaymentToken() { return paymentToken; }

    // Setters
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setCity(String city) { this.city = city; }
    public void setProvince(String province) { this.province = province; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setCountry(String country) { this.country = country; }
    public void setNewsletterSubscribed(boolean newsletterSubscribed) { this.newsletterSubscribed = newsletterSubscribed; }
    public void setOrderNotes(String orderNotes) { this.orderNotes = orderNotes; }
    public void setItems(List<Object> items) { this.items = items; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }
}
