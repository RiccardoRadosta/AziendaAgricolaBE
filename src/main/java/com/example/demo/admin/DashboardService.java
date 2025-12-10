package com.example.demo.admin;

import com.example.demo.admin.dto.DashboardStatsDTO;
import com.example.demo.admin.dto.TopProductDTO;
import com.example.demo.order.Order;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
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
import java.util.function.Predicate;
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
        // 1. Recupera solo gli ordini PADRE per le statistiche finanziarie
        List<Order> parentOrders = firestore.collection("orders")
                .whereEqualTo("type", "PARENT")
                .get().get().toObjects(Order.class);

        // 2. Recupera solo gli ordini FIGLIO per l'analisi dei prodotti
        List<Order> childOrders = firestore.collection("orders")
                .whereEqualTo("type", "CHILD")
                .get().get().toObjects(Order.class);

        // Calcoli basati sugli ordini PADRE
        double revenueToday = calculateTotalRevenue(parentOrders, order -> isToday(order.getCreatedAt().toDate()));
        long ordersToday = countOrders(parentOrders, order -> isToday(order.getCreatedAt().toDate()));

        double revenueThisWeek = calculateTotalRevenue(parentOrders, order -> isThisWeek(order.getCreatedAt().toDate()));
        long ordersThisWeek = countOrders(parentOrders, order -> isThisWeek(order.getCreatedAt().toDate()));

        double revenueThisMonth = calculateTotalRevenue(parentOrders, order -> isThisMonth(order.getCreatedAt().toDate()));
        long ordersThisMonth = countOrders(parentOrders, order -> isThisMonth(order.getCreatedAt().toDate()));

        // Calcolo prodotti più venduti basato sugli ordini FIGLIO
        List<TopProductDTO> topSellingProducts = calculateTopSellingProducts(childOrders);

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

    private double calculateTotalRevenue(List<Order> parentOrders, Predicate<Order> filter) {
        return parentOrders.stream()
                .filter(filter)
                .mapToDouble(Order::getSubtotal) // Ora questo è sicuro perché usiamo solo ordini PADRE
                .sum();
    }

    private long countOrders(List<Order> parentOrders, Predicate<Order> filter) {
        return parentOrders.stream()
                .filter(filter)
                .count(); // Il conteggio ora è corretto
    }

    private List<TopProductDTO> calculateTopSellingProducts(List<Order> childOrders) {
        return childOrders.stream()
            .filter(order -> order.getItems() != null && !order.getItems().isEmpty())
            .flatMap(order -> {
                try {
                    List<Map<String, Object>> items = objectMapper.readValue(order.getItems(), new TypeReference<List<Map<String, Object>>>() {});
                    return items.stream();
                } catch (IOException e) {
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
