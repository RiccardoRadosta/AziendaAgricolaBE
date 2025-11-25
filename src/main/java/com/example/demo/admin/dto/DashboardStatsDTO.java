package com.example.demo.admin.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardStatsDTO {

    private double revenueToday;
    private double revenueThisWeek;
    private double revenueThisMonth;
    private int ordersToday;
    private int ordersThisWeek;
    private int ordersThisMonth;
    private List<TopProductDTO> topSellingProducts;
}
