package com.example.demo.coupon;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Coupon {
    @DocumentId
    private String id;

    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Date expiryDate;
    private boolean active; // Rinominato da isActive
    private int usageLimit;
    private int usageCount;

    public Coupon() {
        // Costruttore vuoto per Firestore
    }
}
