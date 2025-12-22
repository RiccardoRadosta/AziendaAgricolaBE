package com.example.demo.order;

import com.example.demo.coupon.Coupon;
import com.example.demo.coupon.CouponService;
import com.example.demo.coupon.DiscountType;
import com.example.demo.order.dto.OrderCustomerUpdateDTO;
import com.example.demo.order.dto.ShipmentStatusUpdateDTO;
import com.example.demo.product.Product;
import com.example.demo.product.ProductService;
import com.example.demo.settings.Setting;
import com.example.demo.settings.SettingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.WriteBatch;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final Firestore firestore;
    private final ObjectMapper objectMapper;
    private final BrevoEmailService brevoEmailService;
    private final ProductService productService;
    private final SettingService settingService;
    private final CouponService couponService;

    public OrderService(Firestore firestore, BrevoEmailService brevoEmailService, ProductService productService, SettingService settingService, CouponService couponService) {
        this.firestore = firestore;
        this.objectMapper = new ObjectMapper();
        this.brevoEmailService = brevoEmailService;
        this.productService = productService;
        this.settingService = settingService;
        this.couponService = couponService;
    }

    public double calculateOrderTotal(OrderDTO orderDTO) throws ExecutionException, InterruptedException, IOException {
        List<Map<String, Object>> itemsFromDTO = objectMapper.readValue(orderDTO.getItems(), new TypeReference<>() {});
        BigDecimal merchandiseTotal = BigDecimal.ZERO;

        for (Map<String, Object> item : itemsFromDTO) {
            String productId = (String) item.get("id");
            int quantity = ((Number) item.get("quantity")).intValue();

            DocumentSnapshot productDoc = firestore.collection("products").document(productId).get().get();
            if (!productDoc.exists()) {
                 throw new IllegalArgumentException("Product with ID " + productId + " not found.");
            }
            Product product = productDoc.toObject(Product.class);

            double priceToUse = (product.getDiscountPrice() != null && product.getDiscountPrice() > 0)
                ? product.getDiscountPrice()
                : product.getPrice();

            merchandiseTotal = merchandiseTotal.add(BigDecimal.valueOf(priceToUse).multiply(BigDecimal.valueOf(quantity)));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (orderDTO.getCouponCode() != null && !orderDTO.getCouponCode().isEmpty()) {
            Optional<Coupon> couponOpt = couponService.verifyCoupon(orderDTO.getCouponCode());
            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();
                if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
                    BigDecimal percentage = coupon.getDiscountValue().divide(new BigDecimal("100"));
                    discountAmount = merchandiseTotal.multiply(percentage);
                } else { // FIXED_AMOUNT
                    discountAmount = coupon.getDiscountValue();
                }

                if (discountAmount.compareTo(merchandiseTotal) > 0) {
                    discountAmount = merchandiseTotal;
                }
            }
        }
        
        BigDecimal merchandiseAfterDiscount = merchandiseTotal.subtract(discountAmount);

        Setting settings = settingService.getSettings();
        BigDecimal shippingCost = BigDecimal.ZERO;

        BigDecimal freeShippingThreshold = Optional.ofNullable(settings.getFreeShippingThreshold()).orElse(BigDecimal.ZERO);

        if (freeShippingThreshold.compareTo(BigDecimal.ZERO) <= 0 || merchandiseAfterDiscount.compareTo(freeShippingThreshold) < 0) {
            shippingCost = Optional.ofNullable(settings.getStandardShippingCost()).orElse(BigDecimal.ZERO);
            if ("split".equalsIgnoreCase(orderDTO.getShipmentPreference())) {
                 BigDecimal splitCost = Optional.ofNullable(settings.getSplitShippingCost()).orElse(BigDecimal.ZERO);
                 shippingCost = shippingCost.add(splitCost);
            }
        }

        BigDecimal finalTotal = merchandiseAfterDiscount.add(shippingCost);
        
        return finalTotal.doubleValue();
    }

    public void createOrder(OrderDTO orderDTO) throws IOException, ExecutionException, InterruptedException {
        List<Map<String, Object>> allItems = objectMapper.readValue(orderDTO.getItems(), new TypeReference<>() {});

        List<Map<String, Object>> regularItems = new ArrayList<>();
        List<Map<String, Object>> preOrderItems = new ArrayList<>();

        for (Map<String, Object> item : allItems) {
            String productId = (String) item.get("id");
            DocumentSnapshot productDoc = firestore.collection("products").document(productId).get().get();
            if (productDoc.exists()) {
                item.put("price", productDoc.getDouble("price"));
                String preSaleDate = productDoc.getString("preSaleDate");
                if (preSaleDate != null && !preSaleDate.isEmpty()) {
                    preOrderItems.add(item);
                } else {
                    regularItems.add(item);
                }
            } else {
                throw new IllegalArgumentException("Prodotto con ID " + productId + " non trovato.");
            }
        }

        boolean shouldSplit = "split".equalsIgnoreCase(orderDTO.getShipmentPreference()) && !regularItems.isEmpty() && !preOrderItems.isEmpty();

        WriteBatch batch = firestore.batch();

        Order parentOrder = createParentOrder(orderDTO);
        List<String> childOrderIds = new ArrayList<>();

        if (shouldSplit) {
            String regularChildId = "child_" + UUID.randomUUID().toString();
            childOrderIds.add(regularChildId);
            createChildOrder(batch, parentOrder.getId(), regularChildId, regularItems, 0);

            String preOrderChildId = "child_" + UUID.randomUUID().toString();
            childOrderIds.add(preOrderChildId);
            createChildOrder(batch, parentOrder.getId(), preOrderChildId, preOrderItems, 3);

        } else {
            String childId = "child_" + UUID.randomUUID().toString();
            childOrderIds.add(childId);
            int status = preOrderItems.isEmpty() ? 0 : 3;
            createChildOrder(batch, parentOrder.getId(), childId, allItems, status);
        }

        parentOrder.setChildOrderIds(childOrderIds);
        DocumentReference parentRef = firestore.collection("orders").document(parentOrder.getId());
        batch.set(parentRef, parentOrder);

        batch.commit().get();

        brevoEmailService.sendOrderConfirmationEmail(parentOrder, getChildOrders(parentOrder.getId()));

        List<Map<String, Object>> itemsToUpdateStock = shouldSplit ? regularItems : allItems;
        for (Map<String, Object> item : itemsToUpdateStock) {
            productService.decreaseStock((String) item.get("id"), ((Number) item.get("quantity")).intValue());
        }
    }

    private Order createParentOrder(OrderDTO dto) {
        Order parent = new Order();
        parent.setId("ord_" + UUID.randomUUID());
        parent.setType("PARENT");
        parent.setCreatedAt(Timestamp.now());

        parent.setFullName(dto.getFullName());
        parent.setEmail(dto.getEmail());
        parent.setPhone(dto.getPhone());
        parent.setAddress(dto.getAddress());
        parent.setCity(dto.getCity());
        parent.setProvince(dto.getProvince());
        parent.setPostalCode(dto.getPostalCode());
        parent.setCountry(dto.getCountry());
        parent.setNewsletterSubscribed(dto.isNewsletterSubscribed());
        parent.setOrderNotes(dto.getOrderNotes());

        parent.setSubtotal(dto.getSubtotal());
        parent.setShippingCost(dto.getShippingCost());
        parent.setDiscount(dto.getDiscount());
        parent.setCouponCode(dto.getCouponCode());

        parent.setShipmentPreference(dto.getShipmentPreference());
        parent.setStatus("PROCESSING");

        return parent;
    }

    private void createChildOrder(WriteBatch batch, String parentId, String childId, List<Map<String, Object>> items, int status) {
        if (items.isEmpty()) return;

        Order child = new Order();
        child.setId(childId);
        child.setType("CHILD");
        child.setParentOrderId(parentId);
        child.setCreatedAt(Timestamp.now());
        child.setStatus(String.valueOf(status));

        try {
            child.setItems(objectMapper.writeValueAsString(items));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Errore serializzazione articoli per ordine figlio", e);
        }

        double originalSubtotal = items.stream()
                .mapToDouble(item -> ((Number) item.get("price")).doubleValue() * ((Number) item.get("quantity")).intValue())
                .sum();
        child.setOriginalSubtotal(originalSubtotal);

        DocumentReference childRef = firestore.collection("orders").document(childId);
        batch.set(childRef, child);
    }

    public List<Order> getParentOrders(Integer status) throws ExecutionException, InterruptedException {
        Query query = firestore.collection("orders").whereEqualTo("type", "PARENT");

        if (status != null) {
            // Filter logic might be inaccurate as parent status is generic.
            // For precise filtering, one should query the children's status.
            // query = query.whereEqualTo("status", String.valueOf(status));
        }

        List<Order> orders = query.get().get().toObjects(Order.class);

        // Sort in memory by creation date, newest to oldest
        return orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Order getParentOrderWithChildren(String parentId) throws ExecutionException, InterruptedException {
        DocumentSnapshot parentDoc = firestore.collection("orders").document(parentId).get().get();
        if (!parentDoc.exists() || !"PARENT".equals(parentDoc.getString("type"))) {
            throw new RuntimeException("Parent order not found with ID: " + parentId);
        }
        return parentDoc.toObject(Order.class);
    }

    public List<Order> getChildOrders(String parentId) throws ExecutionException, InterruptedException {
        return firestore.collection("orders")
                .whereEqualTo("parentOrderId", parentId)
                .get().get().getDocuments().stream()
                .map(doc -> doc.toObject(Order.class))
                .collect(Collectors.toList());
    }

    public Order updateOrderCustomerDetails(String orderId, OrderCustomerUpdateDTO dto) throws ExecutionException, InterruptedException {
        DocumentReference orderRef = firestore.collection("orders").document(orderId);
        DocumentSnapshot orderDoc = orderRef.get().get();

        if (!orderDoc.exists() || !"PARENT".equals(orderDoc.getString("type"))) {
            throw new RuntimeException("Parent order not found with ID: " + orderId);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", dto.getFullName());
        updates.put("email", dto.getEmail());
        updates.put("phone", dto.getPhone());
        updates.put("address", dto.getAddress());
        updates.put("city", dto.getCity());
        updates.put("province", dto.getProvince());
        updates.put("postalCode", dto.getPostalCode());
        updates.put("country", dto.getCountry());
        updates.put("orderNotes", dto.getOrderNotes());

        orderRef.update(updates).get();
        return orderRef.get().get().toObject(Order.class);
    }

    public Order updateShipmentStatus(String shipmentId, ShipmentStatusUpdateDTO dto) throws ExecutionException, InterruptedException {
        DocumentReference shipmentRef = firestore.collection("orders").document(shipmentId);
        DocumentSnapshot shipmentDoc = shipmentRef.get().get();

        if (!shipmentDoc.exists() || !"CHILD".equals(shipmentDoc.getString("type"))) {
            throw new RuntimeException("Shipment (child order) not found with ID: " + shipmentId);
        }

        Map<String, Object> updates = new HashMap<>();
        if (dto.getStatus() != null) {
            if (dto.getStatus() < 0 || dto.getStatus() > 3) {
                throw new IllegalArgumentException("Invalid shipment status.");
            }
            updates.put("status", String.valueOf(dto.getStatus()));
        }

        if (dto.getTrackingNumber() != null && !dto.getTrackingNumber().trim().isEmpty()) {
            updates.put("trackingNumber", dto.getTrackingNumber().trim());
        }

        if (!updates.isEmpty()) {
            shipmentRef.update(updates).get();
        }

        // TODO: Aggiungere logica invio email qui

        return shipmentRef.get().get().toObject(Order.class);
    }

    public void deleteOrder(String parentId) throws ExecutionException, InterruptedException {
        DocumentSnapshot parentDoc = firestore.collection("orders").document(parentId).get().get();
        if (!parentDoc.exists() || !"PARENT".equals(parentDoc.getString("type"))) {
            throw new RuntimeException("Parent order not found with ID: " + parentId);
        }

        List<String> childIds = (List<String>) parentDoc.get("childOrderIds");

        WriteBatch batch = firestore.batch();

        if (childIds != null) {
            for (String childId : childIds) {
                batch.delete(firestore.collection("orders").document(childId));
            }
        }
        batch.delete(firestore.collection("orders").document(parentId));

        batch.commit().get();
    }
}
