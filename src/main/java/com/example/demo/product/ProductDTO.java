package com.example.demo.product;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductDTO {
    private String id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private List<String> imageUrls;
    private String category;
    private boolean visible;
    private boolean featured;
    private Double discountPrice;
    private String preSaleDate;
    private String ingredients;
    private String origin;
    private String nutrition;
    private Integer vatRate;
}
