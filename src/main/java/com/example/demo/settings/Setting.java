package com.example.demo.settings;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class Setting {
    // Usiamo un ID fisso per avere sempre un solo documento di impostazioni
    public static final String SINGLETON_ID = "current";

    private BigDecimal freeShippingThreshold;
    private BigDecimal standardShippingCost;
    private BigDecimal splitShippingCost; // New field

    public Setting() {
        // Costruttore vuoto necessario per la deserializzazione di Firestore
    }
}
