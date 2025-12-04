package com.example.demo.order;

import com.example.demo.product.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final Firestore firestore;
    private final ObjectMapper objectMapper;
    private final BrevoEmailService brevoEmailService;
    private final ProductService productService; // <-- DIPENDENZA AGGIUNTA

    public OrderService(Firestore firestore, BrevoEmailService brevoEmailService, ProductService productService) {
        this.firestore = firestore;
        this.objectMapper = new ObjectMapper();
        this.brevoEmailService = brevoEmailService;
        this.productService = productService; // <-- DIPENDENZA INIZIALIZZATA
    }

    public List<Order> getAllOrders(Integer status) throws ExecutionException, InterruptedException {
        Query query = firestore.collection("orders").orderBy("orderDate", Query.Direction.DESCENDING);

        if (status != null) {
            query = query.whereEqualTo("orderStatus", status);
        }

        List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();

        return documents.stream()
                .map(doc -> doc.toObject(Order.class))
                .collect(Collectors.toList());
    }

    public void createOrder(OrderDTO orderDTO) {
        Order order = new Order();
        // ... (codice di creazione ordine invariato)
        order.setFullName(orderDTO.getFullName());
        order.setEmail(orderDTO.getEmail());
        order.setPhone(orderDTO.getPhone());
        order.setAddress(orderDTO.getAddress());
        order.setCity(orderDTO.getCity());
        order.setProvince(orderDTO.getProvince());
        order.setPostalCode(orderDTO.getPostalCode());
        order.setCountry(orderDTO.getCountry());
        order.setNewsletterSubscribed(orderDTO.isNewsletterSubscribed());
        order.setOrderNotes(orderDTO.getOrderNotes());
        order.setSubtotal(orderDTO.getSubtotal());
        order.setOrderDate(new Date());
        order.setOrderStatus(0);
        order.setId(UUID.randomUUID().toString());

        try {
            // Specifichiamo che la lista contiene mappe di String/Object
            List<Map<String, Object>> itemsList = objectMapper.readValue(orderDTO.getItems(), new TypeReference<List<Map<String, Object>>>(){});
            order.setItems(new ArrayList<>(itemsList));

            // 1. SALVIAMO l'ordine nel database
            firestore.collection("orders").document(order.getId()).set(order);

            // 2. DECREMENTIAMO lo stock per ogni prodotto nell'ordine
            for (Map<String, Object> item : itemsList) {
                String productId = (String) item.get("id");
                // La quantità può essere Integer o Long, gestiamola in modo sicuro
                Integer quantity = ((Number) item.get("quantity")).intValue();

                if (productId != null && quantity > 0) {
                    productService.decreaseStock(productId, quantity);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Formato items non valido o errore nell'aggiornamento dello stock.", e);
        }

        // 3. INVIAMO l'email di conferma
        brevoEmailService.sendOrderConfirmationEmail(order);
    }

    /**
     * MODIFICA lo stato di un ordine esistente su Firestore.
     * @param orderId L'ID dell'ordine da modificare.
     * @param newStatus Il nuovo stato da impostare.
     * @return L'ordine completo dopo la modifica.
     */
    public Order updateOrderStatus(String orderId, int newStatus) throws ExecutionException, InterruptedException {
        if (newStatus < 0 || newStatus > 2) {
            throw new IllegalArgumentException("Lo stato dell'ordine non è valido.");
        }

        DocumentReference orderRef = firestore.collection("orders").document(orderId);

        // Prima verifichiamo che l'ordine esista
        if (!orderRef.get().get().exists()) {
            throw new RuntimeException("Ordine non trovato con ID: " + orderId);
        }

        // Eseguiamo la MODIFICA del singolo campo 'orderStatus'.
        // Questa operazione NON tocca gli altri campi del documento.
        ApiFuture<WriteResult> updateFuture = orderRef.update("orderStatus", newStatus);
        updateFuture.get(); // Attendiamo che la modifica sia completata con successo

        // Come richiesto dalle specifiche API, dopo la modifica, recuperiamo e restituiamo
        // l'intero oggetto aggiornato per inviarlo al frontend.
        DocumentSnapshot updatedDocument = orderRef.get().get();
        return updatedDocument.toObject(Order.class);
    }

    public Order updateOrderDetails(String orderId, OrderDTO orderDetails) throws ExecutionException, InterruptedException {
        DocumentReference orderRef = firestore.collection("orders").document(orderId);
        DocumentSnapshot document = orderRef.get().get();
        if (!document.exists()) {
            throw new RuntimeException("Ordine non trovato con ID: " + orderId);
        }
        Map<String, Object> updates = new HashMap<>();
        if (orderDetails.getFullName() != null) {
            updates.put("fullName", orderDetails.getFullName());
        }
        if (orderDetails.getEmail() != null) {
            updates.put("email", orderDetails.getEmail());
        }
        if (orderDetails.getPhone() != null) {
            updates.put("phone", orderDetails.getPhone());
        }
        if (orderDetails.getAddress() != null) {
            updates.put("address", orderDetails.getAddress());
        }
        if (orderDetails.getCity() != null) {
            updates.put("city", orderDetails.getCity());
        }
        if (orderDetails.getProvince() != null) {
            updates.put("province", orderDetails.getProvince());
        }
        if (orderDetails.getPostalCode() != null) {
            updates.put("postalCode", orderDetails.getPostalCode());
        }
        if (orderDetails.getCountry() != null) {
            updates.put("country", orderDetails.getCountry());
        }
        if (orderDetails.getOrderNotes() != null) {
            updates.put("orderNotes", orderDetails.getOrderNotes());
        }
        if (!updates.isEmpty()) {
            orderRef.update(updates).get();
        }
        return orderRef.get().get().toObject(Order.class);
    }

    public void deleteOrder(String orderId) throws ExecutionException, InterruptedException {
        DocumentReference orderRef = firestore.collection("orders").document(orderId);
        if (!orderRef.get().get().exists()) {
            throw new RuntimeException("Ordine non trovato con ID: " + orderId);
        }
        orderRef.delete().get();
    }
}
