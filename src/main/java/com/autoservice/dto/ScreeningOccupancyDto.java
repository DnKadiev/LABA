package com.autoservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScreeningOccupancyDto(
        Long screeningId,
        String movieTitle,
        String hallName,
        LocalDateTime startTime,
        int hallCapacity,
        long soldTickets,
        long refundedTickets,
        long availableSeats,
        BigDecimal occupancyPercent
) {
}
