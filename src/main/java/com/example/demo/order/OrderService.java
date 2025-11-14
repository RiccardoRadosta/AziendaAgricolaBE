package com.example.demo.order;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final Firestore firestore;

    public OrderService(Firestore firestore) {
        this.firestore = firestore;
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
        order.setItems(orderDTO.getItems());
        order.setSubtotal(orderDTO.getSubtotal());

        // --- Server-Managed Data ---
        order.setOrderDate(ZonedDateTime.now());
        order.setOrderStatus(0); // 0 = ordinato/in preparazione
        order.setId(UUID.randomUUID().toString()); // Generate a unique ID

        // --- Save to Firestore ---
        firestore.collection("orders").document(order.getId()).set(order);
    }
}
