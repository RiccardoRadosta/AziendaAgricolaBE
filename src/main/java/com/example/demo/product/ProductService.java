package com.example.demo.product;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.example.demo.config.firebase.FirebaseStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class ProductService {

    private final Firestore firestore;
    private final FirebaseStorageService storageService;
    private final CollectionReference productsCollection;

    public ProductService(Firestore firestore, FirebaseStorageService storageService) {
        this.firestore = firestore;
        this.storageService = storageService;
        this.productsCollection = this.firestore.collection("products");
    }

    public Product createProduct(ProductDTO productDTO) throws IOException, ExecutionException, InterruptedException {
        // 1. Carica le immagini e ottieni gli URL
        List<String> imageUrls = new ArrayList<>();
        if (productDTO.getImages() != null && !productDTO.getImages().isEmpty()) {
            for (MultipartFile imageFile : productDTO.getImages()) {
                if (!imageFile.isEmpty()) {
                    String imageUrl = storageService.uploadFile(imageFile);
                    imageUrls.add(imageUrl);
                }
            }
        }

        // 2. Prepara l'oggetto Product
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setImageUrls(imageUrls);

        // 3. Genera un ID e salva su Firestore
        DocumentReference newProductRef = productsCollection.document();
        String newProductId = newProductRef.getId();
        product.setId(newProductId);

        newProductRef.set(product).get(); // Salva l'oggetto nel documento

        return product;
    }
}
