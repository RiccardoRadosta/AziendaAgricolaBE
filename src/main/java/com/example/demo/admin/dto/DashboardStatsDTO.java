package com.example.demo.admin.dto;

import java.util.List;

public class DashboardStatsDTO {

    private double revenueToday;
    private double revenueThisWeek;
    private double revenueThisMonth;
    private int ordersToday;
    private int ordersThisWeek;
    private int ordersThisMonth;
    private List<TopProductDTO> topSellingProducts;

    // Getters and Setters

    public double getRevenueToday() {
        return revenueToday;
    }

    public void setRevenueToday(double revenueToday) {
        this.revenueToday = revenueToday;
    }

    public double getRevenueThisWeek() {
        return revenueThisWeek;
    }

    public void setRevenueThisWeek(double revenueThisWeek) {
        this.revenueThisWeek = revenueThisWeek;
    }

    public double getRevenueThisMonth() {
        return revenueThisMonth;
    }

    public void setRevenueThisMonth(double revenueThisMonth) {
        this.revenueThisMonth = revenueThisMonth;
    }

    public int getOrdersToday() {
        return ordersToday;
    }

    public void setOrdersToday(int ordersToday) {
        this.ordersToday = ordersToday;
    }

    public int getOrdersThisWeek() {
        return ordersThisWeek;
    }

    public void setOrdersThisWeek(int ordersThisWeek) {
        this.ordersThisWeek = ordersThisWeek;
    }

    public int getOrdersThisMonth() {
        return ordersThisMonth;
    }

    public void setOrdersThisMonth(int ordersThisMonth) {
        this.ordersThisMonth = ordersThisMonth;
    }

    public List<TopProductDTO> getTopSellingProducts() {
        return topSellingProducts;
    }

    public void setTopSellingProducts(List<TopProductDTO> topSellingProducts) {
        this.topSellingProducts = topSellingProducts;
    }
}
