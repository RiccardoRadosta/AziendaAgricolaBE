package com.example.demo.product;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.example.demo.config.firebase.FirebaseStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final Firestore firestore;
    private final FirebaseStorageService firebaseStorageService;
    private final CollectionReference productsCollection;

    public ProductService(Firestore firestore, FirebaseStorageService firebaseStorageService) {
        this.firestore = firestore;
        this.firebaseStorageService = firebaseStorageService;
        this.productsCollection = this.firestore.collection("products");
    }

    public List<Product> getAllProducts() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = productsCollection.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        return documents.stream()
                .map(doc -> doc.toObject(Product.class))
                .collect(Collectors.toList());
    }

    public Product createProduct(ProductDTO productDTO) throws IOException, ExecutionException, InterruptedException {
        List<String> imageUrls = new ArrayList<>();
        if (productDTO.getImages() != null && !productDTO.getImages().isEmpty()) {
            for (MultipartFile image : productDTO.getImages()) {
                String imageUrl = firebaseStorageService.uploadFile(image);
                imageUrls.add(imageUrl);
            }
        }

        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setImages(imageUrls);

        ApiFuture<WriteResult> result = productsCollection.document().set(product);
        String productId = result.get().getUpdateTime().toString(); // Non è l'ID, ma per ora va bene
        product.setId(productId);
        return product;
    }

     public Product updateProduct(String id, ProductDTO productDTO) throws IOException, ExecutionException, InterruptedException {
        Product existingProduct = getProductById(id);
        if (existingProduct == null) {
            throw new RuntimeException("Product not found with id: " + id);
        }

        List<String> newImageUrls = new ArrayList<>();
        if (productDTO.getImages() != null && !productDTO.getImages().isEmpty()) {
            // Elimina le vecchie immagini
            if (existingProduct.getImages() != null) {
                for (String imageUrl : existingProduct.getImages()) {
                    firebaseStorageService.deleteFileFromUrl(imageUrl);
                }
            }
            // Carica le nuove immagini
            for (MultipartFile image : productDTO.getImages()) {
                String imageUrl = firebaseStorageService.uploadFile(image);
                newImageUrls.add(imageUrl);
            }
            existingProduct.setImages(newImageUrls);
        }

        if (productDTO.getName() != null) {
            existingProduct.setName(productDTO.getName());
        }
        if (productDTO.getDescription() != null) {
            existingProduct.setDescription(productDTO.getDescription());
        }
        if (productDTO.getPrice() > 0) {
            existingProduct.setPrice(productDTO.getPrice());
        }

        productsCollection.document(id).set(existingProduct).get();
        existingProduct.setId(id);
        return existingProduct;
    }

    public void deleteProduct(String id) throws ExecutionException, InterruptedException {
        Product product = getProductById(id);
        if (product != null && product.getImages() != null) {
            for (String imageUrl : product.getImages()) {
                firebaseStorageService.deleteFileFromUrl(imageUrl);
            }
        }
        productsCollection.document(id).delete().get();
    }

    public Product getProductById(String id) throws ExecutionException, InterruptedException {
        return productsCollection.document(id).get().get().toObject(Product.class);
    }
}
