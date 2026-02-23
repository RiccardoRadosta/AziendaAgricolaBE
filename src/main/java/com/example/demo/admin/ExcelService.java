package com.example.demo.admin;

import com.example.demo.order.Order;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
            CellStyle sectionHeaderStyle = createSectionHeaderStyle(workbook);
            CellStyle centeredStyle = createCenteredStyle(workbook);

            createSummarySheet(workbook, orders, headerCellStyle, boldStyle, sectionHeaderStyle);
            createProductSalesSheet(workbook, orders, headerCellStyle);
            createOrdersListSheet(workbook, orders, headerCellStyle, centeredStyle);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void createSummarySheet(Workbook workbook, List<Order> orders, CellStyle headerStyle, CellStyle boldStyle, CellStyle sectionHeaderStyle) {
        Sheet sheet = workbook.createSheet("Riepilogo Mensile");
        sheet.setDefaultColumnWidth(35);

        // --- CALCOLI GENERALI ---
        double totalGrossRevenue = 0.0; // Lordo incassato
        double totalShippingCollected = 0.0;
        double totalDiscountValue = 0.0;
        double totalPaymentFees = 0.0;
        double totalRealNetRevenue = 0.0; // Netto dopo le fee
        double totalVAT = 0.0; // Totale IVA (prodotti + spedizione)

        for (Order order : orders) {
            double subtotal = Optional.ofNullable(order.getSubtotal()).orElse(0.0);
            double shipping = Optional.ofNullable(order.getShippingCost()).orElse(0.0);
            double discount = Optional.ofNullable(order.getDiscount()).orElse(0.0);
            double fee = Optional.ofNullable(order.getPaymentFee()).orElse(0.0);
            double net = Optional.ofNullable(order.getNetRevenue()).orElse(0.0);

            totalGrossRevenue += subtotal;
            totalShippingCollected += shipping;
            totalDiscountValue += discount;
            totalPaymentFees += fee;
            totalRealNetRevenue += net;

            // Calcolo IVA Spedizione (22%)
            double shippingVAT = shipping - (shipping / 1.22);
            totalVAT += shippingVAT;

            // Calcolo IVA Prodotti
            if (order.getChildOrders() != null) {
                for (Order child : order.getChildOrders()) {
                    try {
                        List<Map<String, Object>> items = objectMapper.readValue(child.getItems(), new TypeReference<>() {});
                        for (Map<String, Object> item : items) {
                            int quantity = ((Number) item.get("quantity")).intValue();
                            double price = ((Number) item.get("price")).doubleValue();
                            if (item.containsKey("discountPrice") && item.get("discountPrice") != null) {
                                price = ((Number) item.get("discountPrice")).doubleValue();
                            }
                            
                            int vatRate = 22;
                            if (item.containsKey("vatRate") && item.get("vatRate") != null) {
                                vatRate = ((Number) item.get("vatRate")).intValue();
                            }

                            double itemTotal = price * quantity;
                            double itemVAT = itemTotal - (itemTotal / (1 + (double)vatRate / 100));
                            totalVAT += itemVAT;
                        }
                    } catch (IOException e) {
                        System.err.println("Error calculating VAT for order " + order.getId());
                    }
                }
            }
        }

        double totalProductValue = totalGrossRevenue - totalShippingCollected;
        double totalTaxable = totalGrossRevenue - totalVAT; // Imponibile Totale

        long uniqueCustomers = orders.stream().map(Order::getEmail).distinct().count();
        long parentOrdersCount = orders.size();
        long childShipmentsCount = orders.stream().map(Order::getChildOrders).filter(Objects::nonNull).mapToLong(List::size).sum();
        double averageOrderValue = (parentOrdersCount > 0) ? totalGrossRevenue / parentOrdersCount : 0;

        // --- CALCOLI PER METODO DI PAGAMENTO ---
        Map<String, PaymentMethodStat> paymentStats = new HashMap<>();
        for (Order order : orders) {
            String method = Optional.ofNullable(order.getPaymentMethod()).orElse("Sconosciuto");
            PaymentMethodStat stat = paymentStats.computeIfAbsent(method, PaymentMethodStat::new);
            stat.addOrder(
                Optional.ofNullable(order.getSubtotal()).orElse(0.0),
                Optional.ofNullable(order.getPaymentFee()).orElse(0.0),
                Optional.ofNullable(order.getNetRevenue()).orElse(0.0)
            );
        }

        // --- COSTRUZIONE FOGLIO ---
        int rowNum = 0;

        // Titolo Principale
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RIEPILOGO DEL BUSINESS MENSILE");
        titleCell.setCellStyle(headerStyle);
        rowNum++;

        // SEZIONE: FLUSSO DI CASSA
        createSectionHeader(sheet, rowNum++, "FLUSSO DI CASSA (CASH FLOW)", sectionHeaderStyle);
        createStatRow(sheet, rowNum++, "Entrate Totali Lorde (Pagato dai Clienti)", totalGrossRevenue, boldStyle);
        createStatRow(sheet, rowNum++, "- Totale Commissioni (Fee)", totalPaymentFees, boldStyle);
        createStatRow(sheet, rowNum++, "= Totale Netto Incassato (Cash in tasca)", totalRealNetRevenue, boldStyle);
        rowNum++;

        // SEZIONE: ANALISI FISCALE
        createSectionHeader(sheet, rowNum++, "ANALISI FISCALE (STIMA)", sectionHeaderStyle);
        createStatRow(sheet, rowNum++, "Totale Imponibile (Netto IVA)", totalTaxable, boldStyle);
        createStatRow(sheet, rowNum++, "+ Totale IVA (Scorporata)", totalVAT, boldStyle);
        createStatRow(sheet, rowNum++, "= Totale Lordo (Verifica)", totalGrossRevenue, boldStyle);
        rowNum++;

        // SEZIONE: DETTAGLIO ENTRATE
        createSectionHeader(sheet, rowNum++, "DETTAGLIO ENTRATE (LORDO)", sectionHeaderStyle);
        createStatRow(sheet, rowNum++, "Valore Prodotti", totalProductValue, boldStyle);
        createStatRow(sheet, rowNum++, "Costi di Spedizione Incassati", totalShippingCollected, boldStyle);
        createStatRow(sheet, rowNum++, "(Valore Totale Sconti Concessi)", totalDiscountValue, boldStyle);
        rowNum++;

        // SEZIONE: DATI OPERATIVI
        createSectionHeader(sheet, rowNum++, "DATI OPERATIVI", sectionHeaderStyle);
        createStatRow(sheet, rowNum++, "Numero Ordini", parentOrdersCount, boldStyle);
        createStatRow(sheet, rowNum++, "Numero Spedizioni Generate", childShipmentsCount, boldStyle);
        createStatRow(sheet, rowNum++, "Numero Clienti Unici", uniqueCustomers, boldStyle);
        createStatRow(sheet, rowNum++, "Valore Medio Ordine (Lordo)", averageOrderValue, boldStyle);
        rowNum++;
        rowNum++;

        // SEZIONE: METODI DI PAGAMENTO (Tabella)
        Row paymentTitleRow = sheet.createRow(rowNum++);
        Cell paymentTitleCell = paymentTitleRow.createCell(0);
        paymentTitleCell.setCellValue("RIEPILOGO PER METODO DI PAGAMENTO");
        paymentTitleCell.setCellStyle(headerStyle);
        
        Row paymentHeaderRow = sheet.createRow(rowNum++);
        String[] paymentHeaders = {"Metodo", "N. Ordini", "Totale Lordo", "Totale Fee", "Totale Netto"};
        for (int i = 0; i < paymentHeaders.length; i++) {
            Cell cell = paymentHeaderRow.createCell(i);
            cell.setCellValue(paymentHeaders[i]);
            cell.setCellStyle(boldStyle);
        }

        for (Map.Entry<String, PaymentMethodStat> entry : paymentStats.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            PaymentMethodStat stat = entry.getValue();
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(stat.getCount());
            row.createCell(2).setCellValue(stat.getTotalGross());
            row.createCell(3).setCellValue(stat.getTotalFee());
            row.createCell(4).setCellValue(stat.getTotalNet());
        }

        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createProductSalesSheet(Workbook workbook, List<Order> orders, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Vendite per Prodotto");

        String[] headers = {"Nome Prodotto", "Quantità Totale Venduta", "Ricavo Generato"};
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
                        String name = (String) item.get("name");
                        int quantity = ((Number) item.get("quantity")).intValue();
                        
                        Object discountPriceObj = item.get("discountPrice");
                        double finalPrice;
                        if (discountPriceObj instanceof Number) {
                            finalPrice = ((Number) discountPriceObj).doubleValue();
                        } else {
                            finalPrice = ((Number) item.get("price")).doubleValue();
                        }

                        ProductSaleStat stat = productStats.computeIfAbsent(name, ProductSaleStat::new);
                        stat.addSale(quantity, finalPrice * quantity);
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
            row.createCell(1).setCellValue(entry.getValue().getTotalQuantity());
            row.createCell(2).setCellValue(entry.getValue().getTotalRevenue());
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createOrdersListSheet(Workbook workbook, List<Order> orders, CellStyle headerStyle, CellStyle centeredStyle) {
        Sheet sheet = workbook.createSheet("Elenco Ordini");

        String[] headers = {
            "Order ID", "Data Ordine", "Cliente", "Email", 
            "Metodo Pagamento", "Totale Ordine (Lordo)", "Commissioni (Fee)", "Netto Reale",
            "Spedizione ID", "Stato Spedizione", "Articolo", "Quantità", "Prezzo Originale", "Prezzo Finale",
            "Aliquota IVA", "Importo IVA", "Imponibile"
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
            if (children == null) children = Collections.emptyList();

            double orderTotal = Optional.ofNullable(order.getSubtotal()).orElse(0.0);
            double paymentFee = Optional.ofNullable(order.getPaymentFee()).orElse(0.0);
            double netRevenue = Optional.ofNullable(order.getNetRevenue()).orElse(0.0);
            String paymentMethod = Optional.ofNullable(order.getPaymentMethod()).orElse("N/A");

            int startOrderRow = rowNum;

            if (children.isEmpty()) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;
                row.createCell(col++).setCellValue(order.getId());
                row.createCell(col++).setCellValue(sdf.format(order.getCreatedAt().toDate()));
                row.createCell(col++).setCellValue(order.getFullName());
                row.createCell(col++).setCellValue(order.getEmail());
                row.createCell(col++).setCellValue(paymentMethod);
                row.createCell(col++).setCellValue(orderTotal);
                row.createCell(col++).setCellValue(paymentFee);
                row.createCell(col++).setCellValue(netRevenue);
            } else {
                for (Order child : children) {
                    try {
                        List<Map<String, Object>> items = objectMapper.readValue(child.getItems(), new TypeReference<>() {});
                        if (items.isEmpty()) items = Collections.singletonList(Collections.emptyMap());

                        int startChildRow = rowNum;

                        for (Map<String, Object> item : items) {
                            Row row = sheet.createRow(rowNum++);
                            int col = 0;
                            
                            // Dati Ordine Padre
                            row.createCell(col++).setCellValue(order.getId());
                            row.createCell(col++).setCellValue(sdf.format(order.getCreatedAt().toDate()));
                            row.createCell(col++).setCellValue(order.getFullName());
                            row.createCell(col++).setCellValue(order.getEmail());
                            row.createCell(col++).setCellValue(paymentMethod);
                            row.createCell(col++).setCellValue(orderTotal);
                            row.createCell(col++).setCellValue(paymentFee);
                            row.createCell(col++).setCellValue(netRevenue);
                            
                            // Dati Spedizione
                            row.createCell(col++).setCellValue(child.getId());
                            row.createCell(col++).setCellValue(convertStatus(child.getStatus()));
                            
                            // Dati Articolo
                            if (!item.isEmpty()) {
                                double originalPrice = ((Number) item.get("price")).doubleValue();
                                int quantity = ((Number) item.get("quantity")).intValue();
                                
                                Object discountPriceObj = item.get("discountPrice");
                                double finalItemPrice;
                                if (discountPriceObj instanceof Number) {
                                    finalItemPrice = ((Number) discountPriceObj).doubleValue();
                                } else {
                                    finalItemPrice = originalPrice;
                                }
                                
                                // Recupera vatRate
                                int vatRate = 22;
                                if (item.containsKey("vatRate") && item.get("vatRate") != null) {
                                    vatRate = ((Number) item.get("vatRate")).intValue();
                                }
                                
                                double itemTotal = finalItemPrice * quantity;
                                double itemVAT = itemTotal - (itemTotal / (1 + (double)vatRate / 100));
                                double itemTaxable = itemTotal - itemVAT;

                                row.createCell(col++).setCellValue((String) item.get("name"));
                                row.createCell(col++).setCellValue(quantity);
                                row.createCell(col++).setCellValue(originalPrice);
                                row.createCell(col++).setCellValue(finalItemPrice);
                                
                                // Nuove colonne IVA
                                row.createCell(col++).setCellValue(vatRate + "%");
                                row.createCell(col++).setCellValue(itemVAT);
                                row.createCell(col++).setCellValue(itemTaxable);
                            }
                        }

                        // Unione celle Spedizione
                        int endChildRow = rowNum - 1;
                        if (endChildRow > startChildRow) {
                            sheet.addMergedRegion(new CellRangeAddress(startChildRow, endChildRow, 8, 8));
                            sheet.addMergedRegion(new CellRangeAddress(startChildRow, endChildRow, 9, 9));
                            sheet.getRow(startChildRow).getCell(8).setCellStyle(centeredStyle);
                            sheet.getRow(startChildRow).getCell(9).setCellStyle(centeredStyle);
                        }

                    } catch (IOException e) {
                        System.err.println("Errore durante la lettura degli articoli per l'ordine figlio: " + child.getId());
                    }
                }
            }

            // Unione celle Ordine Padre
            int endOrderRow = rowNum - 1;
            if (endOrderRow > startOrderRow) {
                for (int i = 0; i <= 7; i++) {
                    sheet.addMergedRegion(new CellRangeAddress(startOrderRow, endOrderRow, i, i));
                    sheet.getRow(startOrderRow).getCell(i).setCellStyle(centeredStyle);
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
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
    
    private CellStyle createSectionHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setUnderline(Font.U_SINGLE);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createCenteredStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void createSectionHeader(Sheet sheet, int rowNum, String label, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);
        cell.setCellValue(label);
        cell.setCellStyle(style);
    }

    private void createStatRow(Sheet sheet, int rowNum, String label, double value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (labelStyle != null) {
            labelCell.setCellStyle(labelStyle);
        }
        if (!label.trim().isEmpty()) {
            row.createCell(1).setCellValue(value);
        }
    }

    private void createStatRow(Sheet sheet, int rowNum, String label, long value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (labelStyle != null) {
            labelCell.setCellStyle(labelStyle);
        }
        if (!label.trim().isEmpty()) {
            row.createCell(1).setCellValue(value);
        }
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

    private static class ProductSaleStat {
        private final String name;
        private int totalQuantity = 0;
        private double totalRevenue = 0.0;

        public ProductSaleStat(String name) {
            this.name = name;
        }

        public void addSale(int quantity, double revenue) {
            this.totalQuantity += quantity;
            this.totalRevenue += revenue;
        }

        public String getName() { return name; }
        public int getTotalQuantity() { return totalQuantity; }
        public double getTotalRevenue() { return totalRevenue; }
    }
    
    private static class PaymentMethodStat {
        private final String method;
        private int count = 0;
        private double totalGross = 0.0;
        private double totalFee = 0.0;
        private double totalNet = 0.0;

        public PaymentMethodStat(String method) {
            this.method = method;
        }

        public void addOrder(double gross, double fee, double net) {
            this.count++;
            this.totalGross += gross;
            this.totalFee += fee;
            this.totalNet += net;
        }

        public int getCount() { return count; }
        public double getTotalGross() { return totalGross; }
        public double getTotalFee() { return totalFee; }
        public double getTotalNet() { return totalNet; }
    }
}
