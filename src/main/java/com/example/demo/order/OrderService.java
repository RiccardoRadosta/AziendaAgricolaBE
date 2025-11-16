package com.example.demo.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final Firestore firestore;
    private final ObjectMapper objectMapper; // Aggiunto per la conversione JSON

    public OrderService(Firestore firestore) {
        this.firestore = firestore;
        this.objectMapper = new ObjectMapper(); // Inizializzato
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

        // --- DTO to Model Mapping ---
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

        // --- Conversione di 'items' da Stringa a Lista ---
        try {
            List<Object> itemsList = objectMapper.readValue(orderDTO.getItems(), new TypeReference<List<Object>>(){});
            order.setItems(itemsList);
        } catch (IOException e) {
            // Gestisci l'eccezione - per ora, impostiamo una lista vuota se la conversione fallisce
            // o potremmo lanciare un'eccezione per indicare un dato malformato.
            e.printStackTrace(); // Logga l'errore per il debug
            throw new IllegalArgumentException("Formato items non valido", e);
        }

        // --- Server-Managed Data ---
        order.setOrderDate(new Date());
        order.setOrderStatus(0);
        order.setId(UUID.randomUUID().toString());

        // --- Save to Firestore ---
        firestore.collection("orders").document(order.getId()).set(order);
    }
}