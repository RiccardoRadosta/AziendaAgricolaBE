package com.example.demo.product;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Product {
    @DocumentId
    private String id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private List<String> imageUrls;
}
