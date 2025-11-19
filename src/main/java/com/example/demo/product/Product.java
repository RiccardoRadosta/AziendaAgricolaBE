package com.example.demo.product;

import com.google.cloud.firestore.annotation.DocumentId;
import java.util.List;

public class Product {
    @DocumentId
    private String id;
    private String name;
    private String description;
    private double price;
    private List<String> imageUrls; // Lista di URL delle immagini

    // Costruttore vuoto necessario per la deserializzazione di Firestore
    public Product() {
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
