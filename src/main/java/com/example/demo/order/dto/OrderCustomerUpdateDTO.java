package com.example.demo.order.dto;

import lombok.Data;

@Data
public class OrderCustomerUpdateDTO {
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private String orderNotes;
}
