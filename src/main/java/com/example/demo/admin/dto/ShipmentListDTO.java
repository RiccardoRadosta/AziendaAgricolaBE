package com.example.demo.admin.dto;

import com.google.cloud.Timestamp;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO "piatto" per rappresentare una singola spedizione (child order)
 * con i dati del destinatario aggregati dal parent order, ottimizzato per il FE.
 */
@Data
@NoArgsConstructor
public class ShipmentListDTO {
    private String id;
    private String parentOrderId;
    private Timestamp shipmentDate;
    private int status;
    private String trackingNumber;
    private String recipientName;
    private String recipientAddress;
    private String recipientCity;
    private String recipientProvince;
    private String recipientPostalCode;
    private String orderNotes;
    private int packageIndex;
    private int totalPackages;
    private List<ShipmentItem> items;
    private Integer richiestaFattura; // Aggiunto per visibilità rapida

    @Data
    @NoArgsConstructor
    public static class ShipmentItem {
        private String id;
        private String name;
        private int quantity;
        private Double price;
        private Double discountPrice;
        private String preSaleDate;
    }
}
