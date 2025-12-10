package com.example.demo.admin;

import com.example.demo.admin.dto.DashboardStatsDTO;
import com.example.demo.admin.dto.TopProductDTO;
import com.example.demo.order.Order;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final Firestore firestore;
    private final ObjectMapper objectMapper;

    public DashboardService(Firestore firestore, ObjectMapper objectMapper) {
        this.firestore = firestore;
        this.objectMapper = objectMapper;
    }

    public DashboardStatsDTO getDashboardStats() throws ExecutionException, InterruptedException {
        List<Order> allOrders = firestore.collection("orders").get().get().getDocuments()
                .stream().map(doc -> doc.toObject(Order.class)).collect(Collectors.toList());

        // Calcoli per periodo
        double revenueToday = calculateTotalRevenue(allOrders, order -> isToday(order.getCreatedAt().toDate()));
        long ordersToday = countOrders(allOrders, order -> isToday(order.getCreatedAt().toDate()));

        double revenueThisWeek = calculateTotalRevenue(allOrders, order -> isThisWeek(order.getCreatedAt().toDate()));
        long ordersThisWeek = countOrders(allOrders, order -> isThisWeek(order.getCreatedAt().toDate()));

        double revenueThisMonth = calculateTotalRevenue(allOrders, order -> isThisMonth(order.getCreatedAt().toDate()));
        long ordersThisMonth = countOrders(allOrders, order -> isThisMonth(order.getCreatedAt().toDate()));

        // Calcolo prodotti più venduti
        List<TopProductDTO> topSellingProducts = calculateTopSellingProducts(allOrders);

        // Costruzione del DTO di risposta
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setRevenueToday(revenueToday);
        stats.setOrdersToday((int) ordersToday);
        stats.setRevenueThisWeek(revenueThisWeek);
        stats.setOrdersThisWeek((int) ordersThisWeek);
        stats.setRevenueThisMonth(revenueThisMonth);
        stats.setOrdersThisMonth((int) ordersThisMonth);
        stats.setTopSellingProducts(topSellingProducts);

        return stats;
    }

    private double calculateTotalRevenue(List<Order> orders, Function<Order, Boolean> filter) {
        return orders.stream()
                .filter(filter::apply)
                .mapToDouble(Order::getSubtotal)
                .sum();
    }

    private long countOrders(List<Order> orders, Function<Order, Boolean> filter) {
        return orders.stream()
                .filter(filter::apply)
                .count();
    }

    private List<TopProductDTO> calculateTopSellingProducts(List<Order> orders) {
        return orders.stream()
            .filter(order -> order.getItems() != null && !order.getItems().isEmpty())
            .flatMap(order -> {
                try {
                    List<Map<String, Object>> items = objectMapper.readValue(order.getItems(), new TypeReference<List<Map<String, Object>>>() {});
                    return items.stream();
                } catch (IOException e) {
                    // Log the error or handle it appropriately
                    return Collections.<Map<String, Object>>emptyList().stream();
                }
            })
            .collect(Collectors.groupingBy(
                itemMap -> (String) itemMap.get("name"),
                Collectors.summingInt(itemMap -> ((Number) itemMap.get("quantity")).intValue())
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(entry -> new TopProductDTO(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }


    // --- Funzioni di utilità per il confronto di date ---

    private boolean isToday(Date date) {
        if (date == null) return false;
        LocalDate orderDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return orderDate.isEqual(LocalDate.now());
    }

    private boolean isThisWeek(Date date) {
        if (date == null) return false;
        LocalDate orderDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return !orderDate.isBefore(startOfWeek) && !orderDate.isAfter(endOfWeek);
    }

    private boolean isThisMonth(Date date) {
        if (date == null) return false;
        LocalDate orderDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        return orderDate.getMonth() == today.getMonth() && orderDate.getYear() == today.getYear();
    }
}
