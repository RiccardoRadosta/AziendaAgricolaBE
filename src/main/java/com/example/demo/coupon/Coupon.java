package com.example.demo.coupon;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Coupon {
    @DocumentId // Mappa l'ID del documento Firestore a questo campo
    private String id;

    private String code; // es. "SCONTO10"
    private String description;
    private DiscountType discountType; // "PERCENTAGE" o "FIXED_AMOUNT"
    private BigDecimal discountValue;
    private Date expiryDate;
    private boolean isActive;
    private int usageLimit; // Quante volte può essere usato in totale
    private int usageCount; // Quante volte è stato già usato

    public Coupon() {
        // Costruttore vuoto per Firestore
    }
}
