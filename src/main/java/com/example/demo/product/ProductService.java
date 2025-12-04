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

    // Corretto: ora imposta l'ID del documento sull'oggetto Product
    public List<Product> getAllProducts() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = productsCollection.whereEqualTo("visible", true).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        return documents.stream()
                .map(doc -> {
                    Product product = doc.toObject(Product.class);
                    product.setId(doc.getId()); // <-- LA CORREZIONE FONDAMENTALE
                    return product;
                })
                .collect(Collectors.toList());
    }
    
    // Corretto: ora imposta l'ID del documento sull'oggetto Product
    public List<Product> getAllProductsForAdmin() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = productsCollection.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        return documents.stream()
                .map(doc -> {
                    Product product = doc.toObject(Product.class);
                    product.setId(doc.getId()); // <-- LA CORREZIONE FONDAMENTALE
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
        updates.put("isFeatured", productDTO.isFeatured());

        docRef.update(updates).get();
    }

    public void deleteProduct(String id) throws ExecutionException, InterruptedException {
        productsCollection.document(id).delete().get();
    }

    /**
     * Decrementa la quantità di stock per un dato prodotto in modo atomico.
     * @param productId L'ID del prodotto da aggiornare.
     * @param quantityToDecrease La quantità da sottrarre dallo stock.
     */
    public void decreaseStock(String productId, int quantityToDecrease) {
        DocumentReference productRef = productsCollection.document(productId);
        // FieldValue.increment è un'operazione atomica fornita da Firestore,
        // che garantisce che la modifica sia sicura anche con più ordini concorrenti.
        // Per decrementare, passiamo un valore negativo.
        productRef.update("stock", FieldValue.increment(-quantityToDecrease));
    }
}
