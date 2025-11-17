package com.example.demo.admin;

import com.example.demo.admin.dto.DashboardStatsDTO;
import com.example.demo.admin.dto.TopProductDTO;
import com.example.demo.order.Order;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final Firestore firestore;

    public DashboardService(Firestore firestore) {
        this.firestore = firestore;
    }

    public DashboardStatsDTO getDashboardStats() throws ExecutionException, InterruptedException {
        List<Order> allOrders = firestore.collection("orders").get().get().getDocuments()
                .stream().map(doc -> doc.toObject(Order.class)).collect(Collectors.toList());

        LocalDate today = LocalDate.now();

        // Calcoli per periodo
        double revenueToday = calculateTotalRevenue(allOrders, order -> isToday(order.getOrderDate()));
        long ordersToday = countOrders(allOrders, order -> isToday(order.getOrderDate()));

        double revenueThisWeek = calculateTotalRevenue(allOrders, order -> isThisWeek(order.getOrderDate()));
        long ordersThisWeek = countOrders(allOrders, order -> isThisWeek(order.getOrderDate()));

        double revenueThisMonth = calculateTotalRevenue(allOrders, order -> isThisMonth(order.getOrderDate()));
        long ordersThisMonth = countOrders(allOrders, order -> isThisMonth(order.getOrderDate()));

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
            .flatMap(order -> ((List<Map<String, Object>>) order.getItems()).stream()) // Corretto per il cast
            .collect(Collectors.groupingBy(
                item -> (String) item.get("name"),
                Collectors.summingInt(item -> ((Number) item.get("quantity")).intValue()) // Corretto per il cast
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(entry -> new TopProductDTO(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }


    // --- Funzioni di utilità per il confronto di date ---

    private boolean isToday(Date date) {
        LocalDate orderDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return orderDate.isEqual(LocalDate.now());
    }

    private boolean isThisWeek(Date date) {
        LocalDate orderDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return !orderDate.isBefore(startOfWeek) && !orderDate.isAfter(endOfWeek);
    }

    private boolean isThisMonth(Date date) {
        LocalDate orderDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        return orderDate.getMonth() == today.getMonth() && orderDate.getYear() == today.getYear();
    }
}
