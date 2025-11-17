package com.example.demo.order;

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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final Firestore firestore;
    private final ObjectMapper objectMapper;

    public OrderService(Firestore firestore) {
        this.firestore = firestore;
        this.objectMapper = new ObjectMapper();
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
        try {
            List<Object> itemsList = objectMapper.readValue(orderDTO.getItems(), new TypeReference<List<Object>>(){});
            order.setItems(itemsList);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Formato items non valido", e);
        }
        order.setOrderDate(new Date());
        order.setOrderStatus(0);
        order.setId(UUID.randomUUID().toString());
        firestore.collection("orders").document(order.getId()).set(order);
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
}
