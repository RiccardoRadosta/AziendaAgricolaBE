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
    private final ProductService productService;

    public OrderService(Firestore firestore, BrevoEmailService brevoEmailService, ProductService productService) {
        this.firestore = firestore;
        this.objectMapper = new ObjectMapper();
        this.brevoEmailService = brevoEmailService;
        this.productService = productService;
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
        order.setShipmentPreference(orderDTO.getShipmentPreference());

        boolean isPreOrder = false;

        try {
            List<Map<String, Object>> itemsList = objectMapper.readValue(orderDTO.getItems(), new TypeReference<List<Map<String, Object>>>() {});
            order.setItems(new ArrayList<>(itemsList));

            for (Map<String, Object> item : itemsList) {
                String productId = (String) item.get("id");
                ApiFuture<DocumentSnapshot> future = firestore.collection("products").document(productId).get();
                DocumentSnapshot document = future.get();
                if (document.exists()) {
                    String preSaleDate = document.getString("preSaleDate");
                    if (preSaleDate != null && !preSaleDate.isEmpty()) {
                        isPreOrder = true;
                        break;
                    }
                }
            }

            if (isPreOrder) {
                order.setOrderStatus(3);
            } else {
                order.setOrderStatus(0);
            }

            order.setId(UUID.randomUUID().toString());
            firestore.collection("orders").document(order.getId()).set(order);

            for (Map<String, Object> item : itemsList) {
                String productId = (String) item.get("id");
                Integer quantity = ((Number) item.get("quantity")).intValue();

                if (productId != null && quantity > 0) {
                    productService.decreaseStock(productId, quantity);
                }
            }

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Formato items non valido o errore nell'aggiornamento dello stock.", e);
        }

        brevoEmailService.sendOrderConfirmationEmail(order);
    }

    public Order updateOrderStatus(String orderId, int newStatus) throws ExecutionException, InterruptedException {
        if (newStatus < 0 || newStatus > 3) { // Updated to allow status 3
            throw new IllegalArgumentException("Lo stato dell'ordine non è valido.");
        }

        DocumentReference orderRef = firestore.collection("orders").document(orderId);

        if (!orderRef.get().get().exists()) {
            throw new RuntimeException("Ordine non trovato con ID: " + orderId);
        }

        ApiFuture<WriteResult> updateFuture = orderRef.update("orderStatus", newStatus);
        updateFuture.get();

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
        // ... (existing update logic)
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
