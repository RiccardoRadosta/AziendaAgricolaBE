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

    public Product createProduct(ProductDTO productDTO, List<MultipartFile> images) throws IOException, ExecutionException, InterruptedException {
        // 1. Crea un riferimento per il nuovo documento per ottenere un ID univoco
        DocumentReference newProductRef = productsCollection.document();
        String newProductId = newProductRef.getId();

        // 2. Carica le immagini utilizzando l'ID del prodotto per creare un percorso univoco
        List<String> imageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile imageFile : images) {
                if (!imageFile.isEmpty()) {
                    // Costruisce un percorso come "products/{productId}/{filename}"
                    String blobName = "products/" + newProductId + "/" + imageFile.getOriginalFilename();
                    String imageUrl = storageService.uploadFile(imageFile, blobName);
                    imageUrls.add(imageUrl);
                }
            }
        }

        // 3. Prepara l'oggetto Product con tutti i dati
        Product product = new Product();
        product.setId(newProductId); // Imposta l'ID generato
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setQuantity(productDTO.getQuantity()); // Mantenuto per la logica di business
        product.setCategory(productDTO.getCategory()); // Mantenuto per la logica di business
        product.setImages(imageUrls); // Corretto qui

        // 4. Salva l'oggetto completo su Firestore
        newProductRef.set(product).get();

        return product;
    }
}
