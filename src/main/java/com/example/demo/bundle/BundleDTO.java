package com.example.demo.bundle;

import lombok.Data;
import java.util.List;

@Data
public class BundleDTO {
    private String name;
    private String shortDescription;
    private String benefits;
    
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
    
    private String validFrom;
    private String validTo;
}
