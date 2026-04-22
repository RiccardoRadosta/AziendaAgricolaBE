package com.example.demo.bundle;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class BundleService {

    private final CollectionReference bundlesCollection;
    private final CollectionReference productsCollection;

    public BundleService(Firestore firestore) {
        this.bundlesCollection = firestore.collection("bundles");
        this.productsCollection = firestore.collection("products");
    }

    public List<Bundle> getAllBundles() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = bundlesCollection.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        return documents.stream().map(doc -> {
            Bundle bundle = doc.toObject(Bundle.class);
            bundle.setId(doc.getId());
            return bundle;
        }).collect(Collectors.toList());
    }

    public List<Bundle> getActiveBundles() throws ExecutionException, InterruptedException {
        LocalDate today = LocalDate.now();
        ApiFuture<QuerySnapshot> future = bundlesCollection.whereEqualTo("active", true).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    Bundle b = doc.toObject(Bundle.class);
                    b.setId(doc.getId());
                    return b;
                })
                .filter(b -> isValidTemporally(b, today))
                .sorted(Comparator.comparingInt(Bundle::getPriority).reversed()
                        .thenComparing((b1, b2) -> compareTimestamps(b2.getUpdatedAt(), b1.getUpdatedAt())))
                .collect(Collectors.toList());
    }

    public String createBundle(BundleDTO dto) throws ExecutionException, InterruptedException {
        validateBundle(dto);
        Bundle bundle = mapDtoToBundle(dto);
        bundle.setCreatedAt(Timestamp.now());
        bundle.setUpdatedAt(Timestamp.now());
        
        ApiFuture<DocumentReference> future = bundlesCollection.add(bundle);
        return future.get().getId();
    }

    public void updateBundle(String id, BundleDTO dto) throws ExecutionException, InterruptedException {
        validateBundle(dto);
        DocumentReference docRef = bundlesCollection.document(id);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", dto.getName());
        updates.put("shortDescription", dto.getShortDescription());
        updates.put("benefits", dto.getBenefits());
        updates.put("name_EN", dto.getName_EN());
        updates.put("shortDescription_EN", dto.getShortDescription_EN());
        updates.put("benefits_EN", dto.getBenefits_EN());
        updates.put("active", dto.isActive());
        updates.put("triggerProductIds", dto.getTriggerProductIds());
        updates.put("bundleProductIds", dto.getBundleProductIds());
        updates.put("discountType", dto.getDiscountType());
        updates.put("discountValue", dto.getDiscountValue());
        updates.put("displayMode", dto.getDisplayMode() != null ? dto.getDisplayMode() : "modal");
        updates.put("priority", dto.getPriority());
        updates.put("validFrom", dto.getValidFrom());
        updates.put("validTo", dto.getValidTo());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        docRef.update(updates).get();
    }

    public void deleteBundle(String id) throws ExecutionException, InterruptedException {
        bundlesCollection.document(id).delete().get();
    }

    private void validateBundle(BundleDTO dto) throws ExecutionException, InterruptedException {
        if (dto.getName() == null || dto.getName().isBlank()) throw new IllegalArgumentException("Name is mandatory");
        if (dto.getShortDescription() == null || dto.getShortDescription().isBlank()) throw new IllegalArgumentException("Short description is mandatory");
        if (dto.getTriggerProductIds() == null || dto.getTriggerProductIds().isEmpty()) throw new IllegalArgumentException("Trigger products are mandatory");
        if (dto.getBundleProductIds() == null || dto.getBundleProductIds().isEmpty()) throw new IllegalArgumentException("Bundle products are mandatory");
        
        if (!"PERCENTAGE".equals(dto.getDiscountType()) && !"FIXED_AMOUNT".equals(dto.getDiscountType())) {
            throw new IllegalArgumentException("Invalid discount type");
        }
        
        if (dto.getDiscountValue() < 0) throw new IllegalArgumentException("Discount value must be >= 0");
        
        if (dto.getDisplayMode() != null && !"modal".equals(dto.getDisplayMode()) && !"inline".equals(dto.getDisplayMode())) {
            throw new IllegalArgumentException("Invalid display mode");
        }

        // Verifica unicità ID
        if (hasDuplicates(dto.getTriggerProductIds())) throw new IllegalArgumentException("Trigger product IDs must be unique");
        if (hasDuplicates(dto.getBundleProductIds())) throw new IllegalArgumentException("Bundle product IDs must be unique");

        // Verifica esistenza prodotti
        verifyProductsExist(dto.getTriggerProductIds());
        verifyProductsExist(dto.getBundleProductIds());

        // Verifica coerenza date
        if (dto.getValidFrom() != null && dto.getValidTo() != null) {
            try {
                LocalDate from = LocalDate.parse(dto.getValidFrom());
                LocalDate to = LocalDate.parse(dto.getValidTo());
                if (from.isAfter(to)) throw new IllegalArgumentException("validFrom must be before validTo");
            } catch (DateTimeParseException ignored) {}
        }
    }

    private boolean isValidTemporally(Bundle b, LocalDate today) {
        try {
            if (b.getValidFrom() != null && !b.getValidFrom().isBlank()) {
                if (today.isBefore(LocalDate.parse(b.getValidFrom()))) return false;
            }
            if (b.getValidTo() != null && !b.getValidTo().isBlank()) {
                if (today.isAfter(LocalDate.parse(b.getValidTo()))) return false;
            }
        } catch (DateTimeParseException e) {
            return true; // Se il formato è errato, lo consideriamo attivo ma ignoriamo il filtro date
        }
        return true;
    }

    private void verifyProductsExist(List<String> ids) throws ExecutionException, InterruptedException {
        for (String id : ids) {
            if (!productsCollection.document(id).get().get().exists()) {
                throw new IllegalArgumentException("Product not found: " + id);
            }
        }
    }

    private boolean hasDuplicates(List<String> list) {
        return new HashSet<>(list).size() < list.size();
    }

    private int compareTimestamps(Timestamp t1, Timestamp t2) {
        if (t1 == null || t2 == null) return 0;
        return t1.compareTo(t2);
    }

    private Bundle mapDtoToBundle(BundleDTO dto) {
        Bundle b = new Bundle();
        b.setName(dto.getName());
        b.setShortDescription(dto.getShortDescription());
        b.setBenefits(dto.getBenefits());
        b.setName_EN(dto.getName_EN());
        b.setShortDescription_EN(dto.getShortDescription_EN());
        b.setBenefits_EN(dto.getBenefits_EN());
        b.setActive(dto.isActive());
        b.setTriggerProductIds(dto.getTriggerProductIds());
        b.setBundleProductIds(dto.getBundleProductIds());
        b.setDiscountType(dto.getDiscountType());
        b.setDiscountValue(dto.getDiscountValue());
        b.setDisplayMode(dto.getDisplayMode() != null ? dto.getDisplayMode() : "modal");
        b.setPriority(dto.getPriority());
        b.setValidFrom(dto.getValidFrom());
        b.setValidTo(dto.getValidTo());
        return b;
    }
}
