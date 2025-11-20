package com.example.demo.product;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final CollectionReference productsCollection;

    public ProductService(Firestore firestore) {
        this.productsCollection = firestore.collection("products");
    }

    public List<Product> getAllProducts() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = productsCollection.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        return documents.stream()
                .map(doc -> doc.toObject(Product.class))
                .collect(Collectors.toList());
    }

    public String createProduct(ProductDTO productDTO) throws ExecutionException, InterruptedException {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStock(productDTO.getStock());
        // --- Correzione qui ---
        product.setImageUrls(productDTO.getImageUrls());

        ApiFuture<DocumentReference> future = productsCollection.add(product);
        return future.get().getId();
    }

    public void updateProduct(String id, ProductDTO productDTO) throws ExecutionException, InterruptedException {
        DocumentReference docRef = productsCollection.document(id);
        
        // Usiamo una mappa per aggiornare solo i campi non nulli, sebbene il DTO li avrà tutti.
        // Questo è un approccio flessibile per futuri aggiornamenti parziali.
        Map<String, Object> updates = Map.of(
            "name", productDTO.getName(),
            "description", productDTO.getDescription(),
            "price", productDTO.getPrice(),
            "stock", productDTO.getStock(),
            "imageUrls", productDTO.getImageUrls()
        );

        docRef.update(updates).get(); // .get() per attendere il completamento
    }

    public void deleteProduct(String id) throws ExecutionException, InterruptedException {
        productsCollection.document(id).delete().get(); // .get() per attendere il completamento
    }
}
