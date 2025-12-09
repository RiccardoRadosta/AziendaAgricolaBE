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
        Query query = couponCollection.whereEqualTo("code", code).whereEqualTo("isActive", true);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
        if (documents.isEmpty()) {
            return Optional.empty();
        }
        // Restituisce il primo coupon trovato che corrisponde
        return Optional.of(documents.get(0).toObject(Coupon.class));
    }

    public Coupon createCoupon(Coupon coupon) throws ExecutionException, InterruptedException {
        // Controlla se un coupon con lo stesso codice esiste già
        Query query = couponCollection.whereEqualTo("code", coupon.getCode());
        if (!query.get().get().isEmpty()) {
            throw new IllegalArgumentException("Un coupon con il codice '" + coupon.getCode() + "' esiste già.");
        }

        // Se l'ID non è impostato, Firestore ne genererà uno automaticamente
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
            long newUsageCount = snapshot.getLong("usageCount") + 1;
            transaction.update(couponRef, "usageCount", newUsageCount);
            return null;
        }).get();
    }    
}
