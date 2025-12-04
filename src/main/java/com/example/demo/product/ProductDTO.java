package com.example.demo.product;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("isFeatured")
    private boolean isFeatured;
}
