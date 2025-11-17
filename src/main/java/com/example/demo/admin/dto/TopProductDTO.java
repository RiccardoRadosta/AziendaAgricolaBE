package com.example.demo.admin.dto;

public class TopProductDTO {

    private String name;
    private int quantitySold;

    public TopProductDTO(String name, int quantitySold) {
        this.name = name;
        this.quantitySold = quantitySold;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(int quantitySold) {
        this.quantitySold = quantitySold;
    }
}
