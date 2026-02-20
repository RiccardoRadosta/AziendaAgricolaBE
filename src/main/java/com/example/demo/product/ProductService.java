package com.example.demo.product;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
        ApiFuture<QuerySnapshot> future = productsCollection.whereEqualTo("visible", true).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        return documents.stream()
                .map(doc -> {
                    Product product = doc.toObject(Product.class);
                    product.setId(doc.getId());
                    return product;
                })
                .collect(Collectors.toList());
    }
    
    public List<Product> getAllProductsForAdmin() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = productsCollection.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        return documents.stream()
                .map(doc -> {
                    Product product = doc.toObject(Product.class);
                    product.setId(doc.getId());
                    return product;
                })
                .collect(Collectors.toList());
    }

    public String createProduct(ProductDTO productDTO) throws ExecutionException, InterruptedException {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStock(productDTO.getStock());
        product.setImageUrls(productDTO.getImageUrls());
        product.setCategory(productDTO.getCategory());
        product.setVisible(productDTO.isVisible());
        product.setFeatured(productDTO.isFeatured());
        product.setDiscountPrice(productDTO.getDiscountPrice());
        product.setPreSaleDate(productDTO.getPreSaleDate());
        product.setIngredients(productDTO.getIngredients());
        product.setOrigin(productDTO.getOrigin());
        product.setNutrition(productDTO.getNutrition());
        
        // Se vatRate è null, usa il default 22 (gestito nella classe Product, ma qui lo settiamo esplicitamente se presente)
        if (productDTO.getVatRate() != null) {
            product.setVatRate(productDTO.getVatRate());
        }

        ApiFuture<DocumentReference> future = productsCollection.add(product);
        return future.get().getId();
    }

    public void updateProduct(String id, ProductDTO productDTO) throws ExecutionException, InterruptedException {
        DocumentReference docRef = productsCollection.document(id);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", productDTO.getName());
        updates.put("description", productDTO.getDescription());
        updates.put("price", productDTO.getPrice());
        updates.put("stock", productDTO.getStock());
        updates.put("imageUrls", productDTO.getImageUrls());
        updates.put("category", productDTO.getCategory());
        updates.put("visible", productDTO.isVisible());
        updates.put("featured", productDTO.isFeatured());
        updates.put("discountPrice", productDTO.getDiscountPrice());
        updates.put("preSaleDate", productDTO.getPreSaleDate());
        updates.put("ingredients", productDTO.getIngredients());
        updates.put("origin", productDTO.getOrigin());
        updates.put("nutrition", productDTO.getNutrition());
        
        if (productDTO.getVatRate() != null) {
            updates.put("vatRate", productDTO.getVatRate());
        }

        docRef.update(updates).get();
    }

    public void deleteProduct(String id) throws ExecutionException, InterruptedException {
        productsCollection.document(id).delete().get();
    }

    public void verifyStockAvailability(List<Map<String, Object>> items) throws ExecutionException, InterruptedException, InsufficientStockException {
        for (Map<String, Object> item : items) {
            String productId = (String) item.get("id");
            Integer quantity = ((Number) item.get("quantity")).intValue();

            if (productId == null || quantity <= 0) {
                continue;
            }

            DocumentReference docRef = productsCollection.document(productId);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (document.exists()) {
                Product product = document.toObject(Product.class);
                if (product != null) {
                    int currentStock = product.getStock();
                    if (currentStock <= 0) {
                        throw new InsufficientStockException(
                            "Product '" + product.getName() + "' is out of stock and cannot be purchased."
                        );
                    }
                    if (currentStock < quantity) {
                        throw new InsufficientStockException(
                            "Insufficient stock for product '" + product.getName() + "'. Requested: " + quantity + ", Available: " + currentStock
                        );
                    }
                }
            } else {
                throw new RuntimeException("Product with ID " + productId + " not found in database.");
            }
        }
    }

    public void decreaseStock(String productId, int quantityToDecrease) {
        DocumentReference productRef = productsCollection.document(productId);
        productRef.update("stock", FieldValue.increment(-quantityToDecrease));
    }
}
