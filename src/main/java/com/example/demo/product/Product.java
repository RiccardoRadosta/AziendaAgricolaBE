package com.example.demo.product;

import com.google.cloud.firestore.annotation.DocumentId;
import java.util.List;

public class Product {
    @DocumentId
    private String id;
    private String name;
    private String description;
    private double price;
    private List<String> images; // Lista di URL delle immagini

    public Product() {
    }

    public Product(String id, String name, String description, double price, List<String> images) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.images = images;
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

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
