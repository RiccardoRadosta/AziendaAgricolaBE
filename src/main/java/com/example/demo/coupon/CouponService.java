package com.example.demo.coupon;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class CouponService {

    private final Firestore firestore;
    private final CollectionReference couponCollection;

    public CouponService(Firestore firestore) {
        this.firestore = firestore;
        this.couponCollection = firestore.collection("coupons");
    }

    public List<Coupon> getAllCoupons() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = couponCollection.orderBy("code").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        return documents.stream()
                .map(doc -> doc.toObject(Coupon.class))
                .collect(Collectors.toList());
    }

    public Optional<Coupon> verifyCoupon(String code) throws ExecutionException, InterruptedException {
        // 1. Query semplice che il tuo Firestore sa già gestire
        ApiFuture<QuerySnapshot> future = couponCollection.whereEqualTo("code", code).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (documents.isEmpty()) {
            return Optional.empty(); // Nessun coupon trovato con questo codice
        }

        // 2. Filtro logico applicato in Java (allineato al resto del progetto)
        Coupon coupon = documents.get(0).toObject(Coupon.class);

        // Se non è attivo, non è valido
        if (!coupon.isActive()) {
            return Optional.empty();
        }

        // Se è scaduto, non è valido
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().before(new java.util.Date())) {
            return Optional.empty();
        }

        // Se ha un limite di utilizzo e questo è stato raggiunto, non è valido
        if (coupon.getUsageLimit() > 0 && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            return Optional.empty();
        }

        // Se tutti i controlli passano, il coupon è valido
        return Optional.of(coupon);
    }

    public Coupon createCoupon(Coupon coupon) throws ExecutionException, InterruptedException {
        Query query = couponCollection.whereEqualTo("code", coupon.getCode());
        if (!query.get().get().isEmpty()) {
            throw new IllegalArgumentException("Un coupon con il codice '" + coupon.getCode() + "' esiste già.");
        }

        DocumentReference docRef = couponCollection.document();
        coupon.setId(docRef.getId());

        docRef.set(coupon).get();
        return coupon;
    }

    public void deleteCoupon(String id) throws ExecutionException, InterruptedException {
        couponCollection.document(id).delete().get();
    }
    
    public void incrementCouponUsage(String couponId) throws ExecutionException, InterruptedException {
        DocumentReference couponRef = couponCollection.document(couponId);
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(couponRef).get();
            // Controlla se il campo esiste prima di incrementarlo
            if (snapshot.contains("usageCount")) {
                long newUsageCount = snapshot.getLong("usageCount") + 1;
                transaction.update(couponRef, "usageCount", newUsageCount);
            } else {
                // Se non esiste, lo crea e lo imposta a 1
                transaction.update(couponRef, "usageCount", 1);
            }
            return null;
        }).get();
    }
}
