package com.example.demo.bundle;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.Data;

import java.util.List;

@Data
public class Bundle {
    @DocumentId
    private String id;
    
    private String name;
    private String shortDescription;
    private String benefits;
    
    // Campi EN
    private String name_EN;
    private String shortDescription_EN;
    private String benefits_EN;
    
    private boolean active = true;
    private List<String> triggerProductIds;
    private List<String> bundleProductIds;
    
    private String discountType; // "PERCENTAGE" | "FIXED_AMOUNT"
    private double discountValue;
    private String displayMode = "modal"; // "modal" | "inline"
    private int priority = 0;
    
    private String validFrom; // yyyy-mm-dd
    private String validTo;   // yyyy-mm-dd
    
    @ServerTimestamp
    private Timestamp createdAt;
    
    @ServerTimestamp
    private Timestamp updatedAt;
}
