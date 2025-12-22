package com.example.demo.order.dto;

import lombok.Data;

@Data
public class ShipmentStatusUpdateDTO {
    private Integer status;
    private String trackingNumber;
}
