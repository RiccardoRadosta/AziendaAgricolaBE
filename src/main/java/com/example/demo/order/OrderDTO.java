package com.example.demo.order;

import java.util.List;

// DTO che mappa la richiesta in arrivo dal frontend
public class OrderDTO {

    // Oggetto innestato per i dati del cliente
    private CustomerDetailsDTO customerDetails;

    // Dati dell'ordine
    private List<Object> items; // Modificato per accettare un vero array di oggetti JSON
    private double subtotal;

    // Dati di pagamento
    private String paymentToken;

    // Getters
    public CustomerDetailsDTO getCustomerDetails() { return customerDetails; }
    public List<Object> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
    public String getPaymentToken() { return paymentToken; }

    // Setters
    public void setCustomerDetails(CustomerDetailsDTO customerDetails) { this.customerDetails = customerDetails; }
    public void setItems(List<Object> items) { this.items = items; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }
}
