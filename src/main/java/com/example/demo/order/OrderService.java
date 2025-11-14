package com.example.demo.order;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class OrderService {

    private static final String COLLECTION_NAME = "orders";

    public void createOrder(OrderDTO orderDTO) {
        Firestore db = FirestoreClient.getFirestore();

        Order order = new Order();
        order.setId(UUID.randomUUID().toString());

        // Legge i dati dall'oggetto innestato customerDetails
        CustomerDetailsDTO customerDetails = orderDTO.getCustomerDetails();
        order.setFullName(customerDetails.getFullName());
        order.setEmail(customerDetails.getEmail());
        order.setPhone(customerDetails.getPhone());
        order.setAddress(customerDetails.getAddress());
        order.setCity(customerDetails.getCity());
        order.setProvince(customerDetails.getProvince());
        order.setPostalCode(customerDetails.getPostalCode());
        order.setCountry(customerDetails.getCountry());
        order.setNewsletterSubscribed(customerDetails.isNewsletterSubscribed());
        order.setOrderNotes(customerDetails.getOrderNotes());

        // Salva l'array di oggetti direttamente
        order.setItems(orderDTO.getItems());
        order.setSubtotal(orderDTO.getSubtotal());

        order.setOrderDate(new Date()); // Imposta la data corrente
        order.setOrderStatus(0); // Status iniziale: "in preparazione"

        ApiFuture<WriteResult> collectionsApiFuture = db.collection(COLLECTION_NAME).document(order.getId()).set(order);

        try {
            collectionsApiFuture.get(); // Attende la conferma di scrittura
        } catch (InterruptedException | ExecutionException e) {
            // In un'app reale, qui dovresti gestire l'errore in modo più robusto
            throw new RuntimeException("Error while saving order to Firestore", e);
        }
    }

    public List<Order> getAllOrders(Integer status) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<Order> orderList = new ArrayList<>();
        ApiFuture<QuerySnapshot> future;

        if (status != null) {
            future = db.collection(COLLECTION_NAME).whereEqualTo("orderStatus", status).get();
        } else {
            future = db.collection(COLLECTION_NAME).get();
        }

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            orderList.add(document.toObject(Order.class));
        }
        return orderList;
    }
}
