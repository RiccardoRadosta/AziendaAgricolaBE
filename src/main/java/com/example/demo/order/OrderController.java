package com.example.demo.order;

import com.example.demo.order.dto.OrderCustomerUpdateDTO;
import com.example.demo.product.InsufficientStockException;
import com.example.demo.product.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {

  private final OrderService orderService;
  private final ProductService productService;
  private final ObjectMapper objectMapper;

  public OrderController(
    OrderService orderService,
    ProductService productService,
    ObjectMapper objectMapper
  ) {
    this.orderService = orderService;
    this.productService = productService;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/orders/charge")
  public ResponseEntity<Map<String, String>> chargeOrder(
    @RequestBody OrderDTO orderDTO
  ) {
    try {
      List<Map<String, Object>> items = objectMapper.readValue(
        orderDTO.getItems(),
        new TypeReference<List<Map<String, Object>>>() {}
      );
      productService.verifyStockAvailability(items);

      // Calcolo sicuro del totale lato server
      double serverTotal = orderService.calculateOrderTotal(orderDTO);
      double clientTotal = orderDTO.getSubtotal();

      // LOGGING PER DEBUG
      System.out.println("--- DEBUG TOTALI ORDINE ---");
      System.out.println("Totale inviato dal Client: " + clientTotal);
      System.out.println("Totale calcolato dal Server: " + serverTotal);
      System.out.println("---------------------------");

      // Confronto con tolleranza per errori in virgola mobile
      if (Math.abs(serverTotal - clientTotal) > 0.01) {
        Map<String, String> response = new HashMap<>();
        response.put(
          "error",
          "Order total mismatch. Client total: " +
          clientTotal +
          ", Server total: " +
          serverTotal
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
      }

      PaymentIntentCreateParams.PaymentMethodOptions.Card cardOptions =
        PaymentIntentCreateParams.PaymentMethodOptions.Card.builder()
          .setRequestThreeDSecure(
            PaymentIntentCreateParams.PaymentMethodOptions.Card.RequestThreeDSecure.ANY
          )
          .build();

      PaymentIntentCreateParams.PaymentMethodOptions paymentMethodOptions =
        PaymentIntentCreateParams.PaymentMethodOptions.builder()
          .setCard(cardOptions)
          .build();

      PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
        .setAmount((long) (serverTotal * 100)) // Usa il totale calcolato dal server
        .setCurrency("eur")
        .setPaymentMethodOptions(paymentMethodOptions)
        .setPaymentMethod(orderDTO.getPaymentToken())
        .build();

      PaymentIntent paymentIntent = PaymentIntent.create(createParams);

      Map<String, String> response = new HashMap<>();
      response.put("status", paymentIntent.getStatus());
      response.put("clientSecret", paymentIntent.getClientSecret());
      return ResponseEntity.ok(response);
    } catch (InsufficientStockException e) {
      Map<String, String> response = new HashMap<>();
      response.put("error", e.getMessage());
      return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    } catch (StripeException e) {
      Map<String, String> response = new HashMap<>();
      response.put(
        "error",
        e.getLocalizedMessage() != null
          ? e.getLocalizedMessage()
          : "An unknown error occurred with Stripe."
      );
      response.put("code", e.getCode());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    } catch (IOException | ExecutionException | InterruptedException e) {
      Map<String, String> response = new HashMap<>();
      response.put(
        "error",
        "Failed to verify stock or process order: " + e.getMessage()
      );
      return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(response);
    }
  }

  @PostMapping("/orders/create")
  public ResponseEntity<Map<String, String>> createOrder(
    @RequestBody OrderDTO orderDTO
  ) {
    try {
      orderService.createOrder(orderDTO);
      Map<String, String> response = new HashMap<>();
      response.put("status", "order_created_successfully");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, String> response = new HashMap<>();
      response.put(
        "error",
        "Failed to create order in database: " + e.getMessage()
      );
      return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(response);
    }
  }

  @GetMapping("/orders")
  public ResponseEntity<List<Order>> getOrders(
    @RequestParam(required = false) Integer status
  ) {
    try {
      List<Order> parentOrders = orderService.getParentOrders(status);
      return ResponseEntity.ok(parentOrders);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/orders/{id}")
  public ResponseEntity<Map<String, Object>> getOrderDetails(
    @PathVariable String id
  ) {
    try {
      Order parentOrder = orderService.getParentOrderWithChildren(id);
      List<Order> childOrders = orderService.getChildOrders(id);

      Map<String, Object> response = new HashMap<>();
      response.put("parent", parentOrder);
      response.put("shipments", childOrders);

      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PutMapping("/orders/{id}")
  public ResponseEntity<Order> updateOrderDetails(
    @PathVariable String id,
    @RequestBody OrderCustomerUpdateDTO dto
  ) {
    try {
      Order updatedOrder = orderService.updateOrderCustomerDetails(id, dto);
      return ResponseEntity.ok(updatedOrder);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PutMapping("/shipments/{id}/status")
  public ResponseEntity<Order> updateShipmentStatus(
    @PathVariable String id,
    @RequestBody Map<String, Integer> body
  ) {
    try {
      Integer newStatus = body.get("status");
      if (newStatus == null) {
        return ResponseEntity.badRequest().build();
      }
      Order updatedShipment = orderService.updateShipmentStatus(id, newStatus);
      return ResponseEntity.ok(updatedShipment);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @DeleteMapping("/orders/{id}")
  public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
    try {
      orderService.deleteOrder(id);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
