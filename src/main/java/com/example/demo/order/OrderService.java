package com.example.demo.order;

import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // UNICA implementazione del metodo createOrder
    public void createOrder(OrderDTO orderDTO) {
        Order orderEntity = new Order();

        // Mappatura corretta da DTO a Entità
        orderEntity.setFullName(orderDTO.getFullName());
        orderEntity.setEmail(orderDTO.getEmail());
        orderEntity.setPhone(orderDTO.getPhone());
        orderEntity.setAddress(orderDTO.getAddress());
        orderEntity.setCity(orderDTO.getCity());
        orderEntity.setProvince(orderDTO.getProvince());
        orderEntity.setPostalCode(orderDTO.getPostalCode());
        orderEntity.setCountry(orderDTO.getCountry());
        orderEntity.setNewsletterSubscribed(orderDTO.isNewsletterSubscribed());
        orderEntity.setOrderNotes(orderDTO.getOrderNotes());
        orderEntity.setItems(orderDTO.getItems());
        orderEntity.setSubtotal(orderDTO.getSubtotal());

        // Dati gestiti dal server
        orderEntity.setOrderDate(ZonedDateTime.now());
        orderEntity.setOrderStatus(2); // 2 = ordinato/in preparazione

        // Salvataggio nel database
        orderRepository.save(orderEntity);
    }
}
