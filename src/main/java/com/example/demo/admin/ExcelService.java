package com.example.demo.admin;

import com.example.demo.order.Order;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    private final ObjectMapper objectMapper;

    public ExcelService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ByteArrayInputStream createExcelReport(List<Order> orders) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerCellStyle = createHeaderStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);

            createSummarySheet(workbook, orders, headerCellStyle, boldStyle);
            createProductSalesSheet(workbook, orders, headerCellStyle);
            createOrdersListSheet(workbook, orders, headerCellStyle);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void createSummarySheet(Workbook workbook, List<Order> orders, CellStyle headerStyle, CellStyle boldStyle) {
        Sheet sheet = workbook.createSheet("Riepilogo Mensile");
        sheet.setDefaultColumnWidth(25);

        // Calcoli
        double totalNetRevenue = orders.stream()
                .mapToDouble(o -> Optional.ofNullable(o.getSubtotal()).orElse(0.0)
                                  + Optional.ofNullable(o.getShippingCost()).orElse(0.0)
                                  - Optional.ofNullable(o.getDiscount()).orElse(0.0))
                .sum();
        double totalGrossMerchandise = orders.stream()
                .mapToDouble(o -> Optional.ofNullable(o.getSubtotal()).orElse(0.0))
                .sum();
        double totalShippingCollected = orders.stream()
                .mapToDouble(o -> Optional.ofNullable(o.getShippingCost()).orElse(0.0))
                .sum();
        double totalDiscountValue = orders.stream()
                .mapToDouble(o -> Optional.ofNullable(o.getDiscount()).orElse(0.0))
                .sum();
        long uniqueCustomers = orders.stream().map(Order::getEmail).distinct().count();
        long parentOrdersCount = orders.size();
        long childShipmentsCount = orders.stream().map(Order::getChildOrders).filter(Objects::nonNull).mapToLong(List::size).sum();
        double averageOrderValue = (parentOrdersCount > 0) ? totalNetRevenue / parentOrdersCount : 0;

        // Scrittura
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Riepilogo del Business Mensile");
        titleCell.setCellStyle(headerStyle);

        int rowNum = 2;
        createStatRow(sheet, rowNum++, "Entrate Totali Nette", totalNetRevenue, boldStyle);
        createStatRow(sheet, rowNum++, "Valore Merce Lordo", totalGrossMerchandise, boldStyle);
        createStatRow(sheet, rowNum++, "Costi di Spedizione Incassati", totalShippingCollected, boldStyle);
        createStatRow(sheet, rowNum++, "Valore Totale Sconti", totalDiscountValue, boldStyle);
        createStatRow(sheet, rowNum++, "Numero Clienti Unici", uniqueCustomers, boldStyle);
        createStatRow(sheet, rowNum++, "Numero Ordini", parentOrdersCount, boldStyle);
        createStatRow(sheet, rowNum++, "Numero Spedizioni Generate", childShipmentsCount, boldStyle);
        createStatRow(sheet, rowNum, "Valore Medio Ordine", averageOrderValue, boldStyle);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createProductSalesSheet(Workbook workbook, List<Order> orders, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Vendite per Prodotto");

        String[] headers = {"ID Prodotto", "Nome Prodotto", "Quantità Totale Venduta", "Ricavo Generato"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        Map<String, ProductSaleStat> productStats = new HashMap<>();
        for (Order order : orders) {
            if (order.getChildOrders() == null) continue;
            for (Order child : order.getChildOrders()) {
                try {
                    List<Map<String, Object>> items = objectMapper.readValue(child.getItems(), new TypeReference<>() {});
                    for (Map<String, Object> item : items) {
                        String id = (String) item.get("id");
                        String name = (String) item.get("name");
                        int quantity = ((Number) item.get("quantity")).intValue();
                        double price = ((Number) item.get("price")).doubleValue();

                        ProductSaleStat stat = productStats.computeIfAbsent(id, k -> new ProductSaleStat(name));
                        stat.addSale(quantity, price);
                    }
                } catch (IOException e) {
                    System.err.println("Error processing items for product sales sheet: " + e.getMessage());
                }
            }
        }

        int rowNum = 1;
        for (Map.Entry<String, ProductSaleStat> entry : productStats.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue().getName());
            row.createCell(2).setCellValue(entry.getValue().getTotalQuantity());
            row.createCell(3).setCellValue(entry.getValue().getTotalRevenue());
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createOrdersListSheet(Workbook workbook, List<Order> orders, CellStyle headerStyle) throws IOException {
        Sheet sheet = workbook.createSheet("Elenco Ordini");

        String[] headers = {
            "Order ID", "Data Ordine", "Cliente", "Email", "Totale Ordine",
            "Spedizione ID", "Stato Spedizione", "Articolo", "Quantità", "Prezzo Unitario"
        };
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (Order order : orders) {
            List<Order> children = order.getChildOrders();
            if (children == null || children.isEmpty()) continue;

            double total = Optional.ofNullable(order.getSubtotal()).orElse(0.0)
                         + Optional.ofNullable(order.getShippingCost()).orElse(0.0)
                         - Optional.ofNullable(order.getDiscount()).orElse(0.0);

            for (Order child : children) {
                try {
                    List<Map<String, Object>> items = objectMapper.readValue(child.getItems(), new TypeReference<>() {});
                    for (Map<String, Object> item : items) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(order.getId());
                        row.createCell(1).setCellValue(sdf.format(order.getCreatedAt().toDate()));
                        row.createCell(2).setCellValue(order.getFullName());
                        row.createCell(3).setCellValue(order.getEmail());
                        row.createCell(4).setCellValue(total);
                        row.createCell(5).setCellValue(child.getId());
                        row.createCell(6).setCellValue(convertStatus(child.getStatus()));
                        row.createCell(7).setCellValue((String) item.get("name"));
                        row.createCell(8).setCellValue(((Number) item.get("quantity")).intValue());
                        row.createCell(9).setCellValue(((Number) item.get("price")).doubleValue());
                    }
                } catch (IOException e) {
                    System.err.println("Errore durante la lettura degli articoli per l'ordine figlio: " + child.getId());
                }
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void createStatRow(Sheet sheet, int rowNum, String label, double value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        row.createCell(1).setCellValue(value);
    }

    private void createStatRow(Sheet sheet, int rowNum, String label, long value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        row.createCell(1).setCellValue(value);
    }

    private String convertStatus(String status) {
        if (status == null) return "Sconosciuto";
        return switch (status) {
            case "0" -> "In attesa di spedizione";
            case "1" -> "Spedito";
            case "2" -> "Consegnato";
            case "3" -> "In pre-ordine";
            default -> "Stato non valido";
        };
    }

    // Classe helper per le statistiche di vendita dei prodotti
    private static class ProductSaleStat {
        private final String name;
        private int totalQuantity = 0;
        private double totalRevenue = 0.0;

        public ProductSaleStat(String name) {
            this.name = name;
        }

        public void addSale(int quantity, double price) {
            this.totalQuantity += quantity;
            this.totalRevenue += (quantity * price);
        }

        public String getName() {
            return name;
        }

        public int getTotalQuantity() {
            return totalQuantity;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }
    }
}
