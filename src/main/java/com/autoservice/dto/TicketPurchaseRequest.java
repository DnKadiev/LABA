package com.autoservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TicketPurchaseRequest(
        @NotNull Long customerId,
        @NotNull @Min(1) Integer seatNumber,
        @DecimalMin("0.0") BigDecimal paidPrice
) {
}
