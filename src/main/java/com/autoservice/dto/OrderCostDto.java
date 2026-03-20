package com.autoservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderCostDto {
    private Long orderId;
    private BigDecimal workTotal;
    private BigDecimal partsTotal;
    private BigDecimal grandTotal;
    private List<ItemCostLine> items;

    public OrderCostDto(Long orderId, BigDecimal workTotal, BigDecimal partsTotal,
                        BigDecimal grandTotal, List<ItemCostLine> items) {
        this.orderId = orderId;
        this.workTotal = workTotal;
        this.partsTotal = partsTotal;
        this.grandTotal = grandTotal;
        this.items = items;
    }

    public Long getOrderId() { return orderId; }
    public BigDecimal getWorkTotal() { return workTotal; }
    public BigDecimal getPartsTotal() { return partsTotal; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public List<ItemCostLine> getItems() { return items; }

    public static class ItemCostLine {
        private String type;
        private String description;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        public ItemCostLine(String type, String description, int quantity,
                            BigDecimal unitPrice, BigDecimal lineTotal) {
            this.type = type;
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getLineTotal() { return lineTotal; }
    }
}
