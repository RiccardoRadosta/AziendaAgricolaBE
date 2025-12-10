package com.example.demo.order;

import com.example.demo.product.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
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

        WriteBatch batch = firestore.batch();

        Order parentOrder = createParentOrder(orderDTO);
        List<String> childOrderIds = new ArrayList<>();

        if (shouldSplit) {
            String regularChildId = "child_" + UUID.randomUUID().toString();
            childOrderIds.add(regularChildId);
            createChildOrder(batch, parentOrder.getId(), regularChildId, regularItems, 0);

            String preOrderChildId = "child_" + UUID.randomUUID().toString();
            childOrderIds.add(preOrderChildId);
            createChildOrder(batch, parentOrder.getId(), preOrderChildId, preOrderItems, 3);

        } else {
            String childId = "child_" + UUID.randomUUID().toString();
            childOrderIds.add(childId);
            int status = preOrderItems.isEmpty() ? 0 : 3;
            createChildOrder(batch, parentOrder.getId(), childId, allItems, status);
        }

        parentOrder.setChildOrderIds(childOrderIds);
        DocumentReference parentRef = firestore.collection("orders").document(parentOrder.getId());
        batch.set(parentRef, parentOrder);

        batch.commit().get();

        // Operazioni post-commit
        brevoEmailService.sendOrderConfirmationEmail(parentOrder, getChildOrders(parentOrder.getId()));

        List<Map<String, Object>> itemsToUpdateStock = shouldSplit ? allItems : allItems;
        for (Map<String, Object> item : itemsToUpdateStock) {
            productService.decreaseStock((String) item.get("id"), ((Number) item.get("quantity")).intValue());
        }
    }

    private Order createParentOrder(OrderDTO dto) {
        Order parent = new Order();
        parent.setId("ord_" + UUID.randomUUID());
        parent.setType("PARENT");
        parent.setCreatedAt(Timestamp.now());

        // Dati cliente
        parent.setFullName(dto.getFullName());
        parent.setEmail(dto.getEmail());
        parent.setPhone(dto.getPhone());
        parent.setAddress(dto.getAddress());
        parent.setCity(dto.getCity());
        parent.setProvince(dto.getProvince());
        parent.setPostalCode(dto.getPostalCode());
        parent.setCountry(dto.getCountry());
        parent.setNewsletterSubscribed(dto.isNewsletterSubscribed());
        parent.setOrderNotes(dto.getOrderNotes());

        // Dati finanziari
        parent.setSubtotal(dto.getSubtotal());
        parent.setShippingCost(dto.getShippingCost());
        parent.setDiscount(dto.getDiscount());
        parent.setCouponCode(dto.getCouponCode());

        parent.setShipmentPreference(dto.getShipmentPreference());
        // Lo stato del padre potrebbe essere una logica aggregata, per ora lo lasciamo semplice
        parent.setStatus("PROCESSING");

        return parent;
    }

    private void createChildOrder(WriteBatch batch, String parentId, String childId, List<Map<String, Object>> items, int status) {
        if (items.isEmpty()) return;

        Order child = new Order();
        child.setId(childId);
        child.setType("CHILD");
        child.setParentOrderId(parentId);
        child.setCreatedAt(Timestamp.now());
        child.setStatus(String.valueOf(status));

        try {
            child.setItems(objectMapper.writeValueAsString(items));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Errore serializzazione articoli per ordine figlio", e);
        }

        double originalSubtotal = items.stream()
                .mapToDouble(item -> ((Number) item.get("price")).doubleValue() * ((Number) item.get("quantity")).intValue())
                .sum();
        child.setOriginalSubtotal(originalSubtotal);

        DocumentReference childRef = firestore.collection("orders").document(childId);
        batch.set(childRef, child);
    }

    public List<Order> getParentOrders(Integer status) throws ExecutionException, InterruptedException {
        Query query = firestore.collection("orders")
                .whereEqualTo("type", "PARENT")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        if (status != null) {
            // Nota: per ora questo filtra sullo stato del padre, che è generico.
            // Una logica più fine richiederebbe di interrogare lo stato dei figli.
            // query = query.whereEqualTo("status", String.valueOf(status));
        }

        return query.get().get().getDocuments().stream()
                .map(doc -> doc.toObject(Order.class))
                .collect(Collectors.toList());
    }

    public Order getParentOrderWithChildren(String parentId) throws ExecutionException, InterruptedException {
        DocumentSnapshot parentDoc = firestore.collection("orders").document(parentId).get().get();
        if (!parentDoc.exists() || !"PARENT".equals(parentDoc.getString("type"))) {
            throw new RuntimeException("Ordine padre non trovato con ID: " + parentId);
        }
        Order parentOrder = parentDoc.toObject(Order.class);
        // La lista degli ID dei figli è già nel padre, non la carichiamo qui ma la usiamo
        return parentOrder;
    }

    public List<Order> getChildOrders(String parentId) throws ExecutionException, InterruptedException {
        return firestore.collection("orders")
                .whereEqualTo("parentOrderId", parentId)
                .get().get().getDocuments().stream()
                .map(doc -> doc.toObject(Order.class))
                .collect(Collectors.toList());
    }

    public Order updateShipmentStatus(String shipmentId, int newStatus) throws ExecutionException, InterruptedException {
        if (newStatus < 0 || newStatus > 3) {
            throw new IllegalArgumentException("Lo stato della spedizione non è valido.");
        }

        DocumentReference shipmentRef = firestore.collection("orders").document(shipmentId);
        DocumentSnapshot shipmentDoc = shipmentRef.get().get();

        if (!shipmentDoc.exists() || !"CHILD".equals(shipmentDoc.getString("type"))) {
            throw new RuntimeException("Spedizione (ordine figlio) non trovata con ID: " + shipmentId);
        }

        shipmentRef.update("status", String.valueOf(newStatus)).get();

        // Potremmo voler aggiornare lo stato aggregato del padre qui

        return shipmentRef.get().get().toObject(Order.class);
    }

    public void deleteOrder(String parentId) throws ExecutionException, InterruptedException {
        DocumentSnapshot parentDoc = firestore.collection("orders").document(parentId).get().get();
        if (!parentDoc.exists() || !"PARENT".equals(parentDoc.getString("type"))) {
            throw new RuntimeException("Ordine padre non trovato con ID: " + parentId);
        }

        List<String> childIds = (List<String>) parentDoc.get("childOrderIds");

        WriteBatch batch = firestore.batch();

        if (childIds != null) {
            for (String childId : childIds) {
                batch.delete(firestore.collection("orders").document(childId));
            }
        }
        batch.delete(firestore.collection("orders").document(parentId));

        batch.commit().get();
    }
}
