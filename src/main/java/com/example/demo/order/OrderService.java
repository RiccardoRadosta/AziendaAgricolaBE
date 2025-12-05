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

    public void createOrder(OrderDTO orderDTO) throws IOException, ExecutionException, InterruptedException {
        List<Map<String, Object>> allItems = objectMapper.readValue(orderDTO.getItems(), new TypeReference<>() {});

        List<Map<String, Object>> regularItems = new ArrayList<>();
        List<Map<String, Object>> preOrderItems = new ArrayList<>();

        for (Map<String, Object> item : allItems) {
            String productId = (String) item.get("id");
            DocumentSnapshot productDoc = firestore.collection("products").document(productId).get().get();

            if (productDoc.exists()) {
                item.put("price", productDoc.getDouble("price"));
                String preSaleDate = productDoc.getString("preSaleDate");

                if (preSaleDate != null && !preSaleDate.isEmpty()) {
                    preOrderItems.add(item);
                } else {
                    regularItems.add(item);
                }
            } else {
                throw new IllegalArgumentException("Prodotto con ID " + productId + " non trovato.");
            }
        }

        boolean shouldSplit = "split".equalsIgnoreCase(orderDTO.getShipmentPreference()) && !regularItems.isEmpty() && !preOrderItems.isEmpty();

        if (shouldSplit) {
            createAndSaveOrder(orderDTO, regularItems, 0);
            createAndSaveOrder(orderDTO, preOrderItems, 3);
        } else {
            int status = preOrderItems.isEmpty() ? 0 : 3;
            createAndSaveOrder(orderDTO, allItems, status);
        }
    }

    private void createAndSaveOrder(OrderDTO baseDto, List<Map<String, Object>> items, int status) {
        if (items.isEmpty()) {
            return;
        }

        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setFullName(baseDto.getFullName());
        order.setEmail(baseDto.getEmail());
        order.setPhone(baseDto.getPhone());
        order.setAddress(baseDto.getAddress());
        order.setCity(baseDto.getCity());
        order.setProvince(baseDto.getProvince());
        order.setPostalCode(baseDto.getPostalCode());
        order.setCountry(baseDto.getCountry());
        order.setNewsletterSubscribed(baseDto.isNewsletterSubscribed());
        order.setOrderNotes(baseDto.getOrderNotes());
        order.setShipmentPreference(baseDto.getShipmentPreference());
        order.setOrderDate(new Date());
        order.setOrderStatus(status);
        order.setItems(new ArrayList<>(items)); // <-- CORRECTED LINE

        double subtotal = items.stream().mapToDouble(item -> {
            double price = ((Number) item.get("price")).doubleValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            return price * quantity;
        }).sum();
        order.setSubtotal(subtotal);

        try {
            firestore.collection("orders").document(order.getId()).set(order).get();

            for (Map<String, Object> item : items) {
                String productId = (String) item.get("id");
                int quantity = ((Number) item.get("quantity")).intValue();
                productService.decreaseStock(productId, quantity);
            }

            brevoEmailService.sendOrderConfirmationEmail(order);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException("Fallimento nel salvataggio dell'ordine o aggiornamento stock per l'ordine ID: " + order.getId(), e);
        }
    }


    public Order updateOrderStatus(String orderId, int newStatus) throws ExecutionException, InterruptedException {
        if (newStatus < 0 || newStatus > 3) {
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
        if (orderDetails.getFullName() != null) {
            updates.put("fullName", orderDetails.getFullName());
        }
        // ... (altri campi)
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
