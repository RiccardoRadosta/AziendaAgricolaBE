package com.example.demo.order;

import com.example.demo.admin.dto.ShipmentListDTO;
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
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final Firestore firestore;
    private final ObjectMapper objectMapper;
    private final BrevoEmailService brevoEmailService;
    private final ProductService productService;
    private final SettingService settingService;
    private final CouponService couponService;

    private static final String STATUS_CONSEGNATO = "2";

    public OrderService(Firestore firestore, BrevoEmailService brevoEmailService, ProductService productService, SettingService settingService, CouponService couponService) {
        this.firestore = firestore;
        this.objectMapper = new ObjectMapper();
        this.brevoEmailService = brevoEmailService;
        this.productService = productService;
        this.settingService = settingService;
        this.couponService = couponService;
    }

    public boolean hasOrdersInPeriod(int month, int year) throws ExecutionException, InterruptedException {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month - 1, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startDate = cal.getTime();

        cal.add(Calendar.MONTH, 1);
        Date endDate = cal.getTime();

        Timestamp startTimestamp = Timestamp.of(startDate);
        Timestamp endTimestamp = Timestamp.of(endDate);

        Query query = firestore.collection("orders")
                .whereEqualTo("type", "PARENT")
                .whereGreaterThanOrEqualTo("createdAt", startTimestamp)
                .whereLessThan("createdAt", endTimestamp)
                .limit(1);

        ApiFuture<QuerySnapshot> future = query.get();
        QuerySnapshot snapshot = future.get();

        return !snapshot.isEmpty();
    }

    public List<Order> searchOrders(String type, String value) throws ExecutionException, InterruptedException {
        Set<Order> foundOrders = new HashSet<>();
        String searchValue = value.toLowerCase().trim();

        switch (type) {
            case "order_id":
                if (value.startsWith("ord_")) {
                    DocumentSnapshot doc = firestore.collection("orders").document(value).get().get();
                    if (doc.exists() && "PARENT".equals(doc.getString("type"))) {
                        foundOrders.add(doc.toObject(Order.class));
                    }
                }
                break;
            case "shipment_id":
                if (value.startsWith("child_")) {
                    DocumentSnapshot childDoc = firestore.collection("orders").document(value).get().get();
                    if (childDoc.exists() && "CHILD".equals(childDoc.getString("type"))) {
                        String parentId = childDoc.getString("parentOrderId");
                        if (parentId != null) {
                            Order parentOrder = getParentOrderWithChildren(parentId);
                            if(parentOrder != null) foundOrders.add(parentOrder);
                        }
                    }
                }
                break;
             case "tracking_number":
                List<QueryDocumentSnapshot> childDocs = firestore.collection("orders")
                    .whereEqualTo("type", "CHILD")
                    .whereEqualTo("trackingNumber", value.trim())
                    .get().get().getDocuments();

                for (QueryDocumentSnapshot childDoc : childDocs) {
                    String parentId = childDoc.getString("parentOrderId");
                    if (parentId != null) {
                        Order parentOrder = getParentOrderWithChildren(parentId);
                        if(parentOrder != null) foundOrders.add(parentOrder);
                    }
                }
                break;
            case "email":
                firestore.collection("orders")
                    .whereEqualTo("type", "PARENT")
                    .whereEqualTo("email_lowercase", searchValue)
                    .get().get()
                    .toObjects(Order.class)
                    .forEach(foundOrders::add);
                break;
            case "name":
                Query query = firestore.collection("orders").whereEqualTo("type", "PARENT");
                List<String> keywords = Arrays.asList(searchValue.split("\\s+"));
                for (String keyword : keywords) {
                    if (!keyword.isEmpty()) {
                        query = query.whereArrayContains("searchKeywords", keyword);
                    }
                }
                 query.get().get().toObjects(Order.class).forEach(foundOrders::add);
                break;
            default:
                // Type not supported
                break;
        }

        return new ArrayList<>(foundOrders);
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

        if (dto.getEmail() != null) {
            parent.setEmail_lowercase(dto.getEmail().toLowerCase());
        }

        if (dto.getFullName() != null && !dto.getFullName().isEmpty()) {
            List<String> keywords = Arrays.stream(dto.getFullName().toLowerCase().split("\\s+"))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            parent.setSearchKeywords(keywords);
        }

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
        }

        List<Order> orders = query.get().get().toObjects(Order.class);

        return orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Order getParentOrderWithChildren(String parentId) throws ExecutionException, InterruptedException {
        DocumentSnapshot parentDoc = firestore.collection("orders").document(parentId).get().get();
        if (!parentDoc.exists() || !"PARENT".equals(parentDoc.getString("type"))) {
            return null;
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

        if (dto.getEmail() != null) {
            updates.put("email_lowercase", dto.getEmail().toLowerCase());
        }

        if (dto.getFullName() != null && !dto.getFullName().isEmpty()) {
            List<String> keywords = Arrays.stream(dto.getFullName().toLowerCase().split("\\s+"))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            updates.put("searchKeywords", keywords);
        }

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

        Order updatedShipment = shipmentRef.get().get().toObject(Order.class);

        boolean isShippedNow = dto.getStatus() != null && dto.getStatus() == 1;
        boolean hasTracking = updatedShipment.getTrackingNumber() != null && !updatedShipment.getTrackingNumber().isEmpty();

        if (isShippedNow && hasTracking) {
            Order parentOrder = getParentOrderWithChildren(updatedShipment.getParentOrderId());
            brevoEmailService.sendShippedOrderEmail(parentOrder, updatedShipment);
        }

        return updatedShipment;
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

    public List<ShipmentListDTO> getShipmentsForAdminList() throws ExecutionException, InterruptedException {
        // Query 1: Fetch all active child orders (shipments), not delivered
        List<Order> childOrders = firestore.collection("orders")
                .whereEqualTo("type", "CHILD")
                .whereNotEqualTo("status", STATUS_CONSEGNATO)
                .get().get().toObjects(Order.class);

        if (childOrders.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect all unique parent and product IDs for efficient fetching
        List<String> parentOrderIds = childOrders.stream()
                .map(Order::getParentOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> productIds = childOrders.stream()
                .flatMap(child -> {
                    try {
                        List<Map<String, Object>> items = objectMapper.readValue(child.getItems(), new TypeReference<List<Map<String, Object>>>() {});
                        return items.stream().map(item -> (String) item.get("id"));
                    } catch (IOException e) {
                        System.err.println("Failed to parse items from child order: " + child.getId());
                        return Stream.<String>empty();
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // Fetch all parent orders and products in bulk using 'IN' queries
        Map<String, Order> parentOrdersMap = fetchInPartitions(parentOrderIds, "orders", Order.class);
        Map<String, Product> productsMap = fetchProductsInPartitions(productIds);

        // Build the final DTO list
        return childOrders.stream()
            .map(child -> {
                Order parent = parentOrdersMap.get(child.getParentOrderId());
                if (parent == null) {
                    System.err.println("Parent order not found for child: " + child.getId());
                    return null; 
                }
                return createShipmentDTO(child, parent, productsMap);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private ShipmentListDTO createShipmentDTO(Order child, Order parent, Map<String, Product> productsMap) {
        ShipmentListDTO dto = new ShipmentListDTO();
        dto.setId(child.getId());
        dto.setParentOrderId(parent.getId());
        dto.setShipmentDate(child.getCreatedAt());
        dto.setStatus(Integer.parseInt(child.getStatus()));
        dto.setTrackingNumber(child.getTrackingNumber());

        dto.setRecipientName(parent.getFullName());
        dto.setRecipientAddress(parent.getAddress());
        dto.setRecipientCity(parent.getCity());
        dto.setRecipientProvince(parent.getProvince());
        dto.setRecipientPostalCode(parent.getPostalCode());
        dto.setOrderNotes(parent.getOrderNotes());

        int totalPackages = parent.getChildOrderIds() != null ? parent.getChildOrderIds().size() : 1;
        int packageIndex = parent.getChildOrderIds() != null ? parent.getChildOrderIds().indexOf(child.getId()) + 1 : 1;
        dto.setTotalPackages(totalPackages);
        dto.setPackageIndex(packageIndex > 0 ? packageIndex : 1);

        try {
            List<Map<String, Object>> itemsFromChild = objectMapper.readValue(child.getItems(), new TypeReference<>() {});
            List<ShipmentListDTO.ShipmentItem> shipmentItems = itemsFromChild.stream()
                    .map(itemMap -> createShipmentItem(itemMap, productsMap))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            dto.setItems(shipmentItems);
        } catch (IOException e) {
            System.err.println("Failed to parse items for DTO creation for child: " + child.getId());
            dto.setItems(Collections.emptyList());
        }

        return dto;
    }

    private ShipmentListDTO.ShipmentItem createShipmentItem(Map<String, Object> itemMap, Map<String, Product> productsMap) {
        String productId = (String) itemMap.get("id");
        Product product = productsMap.get(productId);

        if (product == null) {
            System.err.println("Product with ID " + productId + " not found in pre-fetched map.");
            return null; 
        }

        ShipmentListDTO.ShipmentItem shipmentItem = new ShipmentListDTO.ShipmentItem();
        shipmentItem.setId(product.getId());
        shipmentItem.setName(product.getName());
        shipmentItem.setQuantity(((Number) itemMap.get("quantity")).intValue());
        shipmentItem.setPrice(product.getPrice());
        shipmentItem.setDiscountPrice(product.getDiscountPrice());
        shipmentItem.setPreSaleDate(product.getPreSaleDate());
        return shipmentItem;
    }

    private <T> Map<String, T> fetchInPartitions(List<String> ids, String collection, Class<T> clazz) throws ExecutionException, InterruptedException {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, T> results = new HashMap<>();
        List<List<String>> partitions = partitionList(ids, 30);
        
        for (List<String> partition : partitions) {
            if (partition.isEmpty()) continue;
            firestore.collection(collection).whereIn(FieldPath.documentId(), partition).get().get()
                .forEach(doc -> results.put(doc.getId(), doc.toObject(clazz)));
        }
        return results;
    }

    private Map<String, Product> fetchProductsInPartitions(List<String> ids) throws ExecutionException, InterruptedException {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, Product> results = new HashMap<>();
        List<List<String>> partitions = partitionList(ids, 30);
        
        for (List<String> partition : partitions) {
            if (partition.isEmpty()) continue;
            firestore.collection("products").whereIn(FieldPath.documentId(), partition).get().get()
                .forEach(doc -> {
                    Product p = doc.toObject(Product.class);
                    p.setId(doc.getId());
                    results.put(doc.getId(), p);
                });
        }
        return results;
    }

    private <T> List<List<T>> partitionList(List<T> list, int size) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
        }
        return partitions;
    }
}
