package com.example.demo.product;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private List<String> imageUrls;
    private String category;
    private boolean visible = true;
    private boolean featured = false;
    private Double discountPrice;
    private String preSaleDate;
}
