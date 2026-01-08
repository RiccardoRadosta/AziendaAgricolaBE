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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExcelService {

    private final ObjectMapper objectMapper;

    public ExcelService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ByteArrayInputStream createExcelReport(List<Order> orders) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Report Ordini");

            // Stile per l'intestazione
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Intestazione
            String[] headers = {
                "Order ID", "Data Ordine", "Cliente", "Email", "Totale Ordine",
                "Spedizione ID", "Stato Spedizione", "Articolo", "Quantità", "Prezzo Unitario"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            for (Order order : orders) {
                List<Order> children = order.getChildOrders(); // Assumiamo che siano già caricati
                if (children == null || children.isEmpty()) continue;

                for (Order child : children) {
                    try {
                        List<Map<String, Object>> items = objectMapper.readValue(child.getItems(), new TypeReference<>() {});
                        for (Map<String, Object> item : items) {
                            Row row = sheet.createRow(rowNum++);

                            // Calcolo sicuro del totale ordine
                            double subtotal = Optional.ofNullable(order.getSubtotal()).orElse(0.0);
                            double shippingCost = Optional.ofNullable(order.getShippingCost()).orElse(0.0);
                            double discount = Optional.ofNullable(order.getDiscount()).orElse(0.0);
                            double total = subtotal + shippingCost - discount;

                            row.createCell(0).setCellValue(order.getId());
                            row.createCell(1).setCellValue(sdf.format(order.getCreatedAt().toDate()));
                            row.createCell(2).setCellValue(order.getFullName());
                            row.createCell(3).setCellValue(order.getEmail());
                            row.createCell(4).setCellValue(total);

                            row.createCell(5).setCellValue(child.getId());
                            row.createCell(6).setCellValue(convertStatus(child.getStatus()));

                            row.createCell(7).setCellValue((String) item.get("name")); // Assumiamo che 'name' sia nel JSON
                            row.createCell(8).setCellValue(((Number) item.get("quantity")).intValue());
                            row.createCell(9).setCellValue(((Number) item.get("price")).doubleValue());
                        }
                    } catch (IOException e) {
                        // Logga l'errore o gestiscilo come preferisci
                        System.err.println("Errore durante la lettura degli articoli per l'ordine figlio: " + child.getId());
                    }
                }
            }

            // Adatta larghezza colonne
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private String convertStatus(String status) {
        if (status == null) return "Sconosciuto";
        switch (status) {
            case "0": return "In attesa di spedizione";
            case "1": return "Spedito";
            case "2": return "Consegnato";
            case "3": return "In pre-ordine";
            default: return "Stato non valido";
        }
    }
}
