package com.example.demo.product;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class ProductService {

    private final CollectionReference productsCollection;

    public ProductService(Firestore firestore) {
        this.productsCollection = firestore.collection("products");
    }

    public String createProduct(ProductDTO productDTO) throws ExecutionException, InterruptedException {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStock(productDTO.getStock());
        product.setImageUrl(productDTO.getImageUrl());

        ApiFuture<DocumentReference> future = productsCollection.add(product);
        return future.get().getId();
    }
}
