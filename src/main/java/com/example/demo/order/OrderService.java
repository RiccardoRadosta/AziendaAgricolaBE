package com.example.demo.order;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class OrderService {

    private final Firestore firestore;

    public OrderService(Firestore firestore) {
        this.firestore = firestore;
    }

    public Order createOrder(Order order) throws ExecutionException, InterruptedException {
        ApiFuture<DocumentReference> future = firestore.collection("orders").add(order);
        DocumentReference documentReference = future.get();
        order.setId(documentReference.getId());
        // Ora aggiorna il documento con il suo ID
        firestore.collection("orders").document(documentReference.getId()).set(order).get();
        return order;
    }

    public List<Order> getAllOrders(Integer status) throws ExecutionException, InterruptedException {
        CollectionReference ordersCollection = firestore.collection("orders");
        Query query = ordersCollection;

        if (status != null) {
            query = ordersCollection.whereEqualTo("orderStatus", status);
        }

        ApiFuture<QuerySnapshot> future = query.get();
        QuerySnapshot querySnapshot = future.get();
        List<Order> orders = new ArrayList<>();
        for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
            orders.add(document.toObject(Order.class));
        }
        return orders;
    }

    public Order updateOrderStatus(String orderId, int status) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("orders").document(orderId);
        ApiFuture<WriteResult> future = docRef.update("orderStatus", status);
        future.get(); // Attendi il completamento dell'aggiornamento

        // Recupera e restituisci l'ordine aggiornato
        ApiFuture<DocumentSnapshot> documentSnapshotApiFuture = docRef.get();
        DocumentSnapshot documentSnapshot = documentSnapshotApiFuture.get();
        if (documentSnapshot.exists()) {
            return documentSnapshot.toObject(Order.class);
        } else {
            throw new RuntimeException("Order not found after update with id: " + orderId);
        }
    }
}
