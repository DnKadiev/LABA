package com.autoservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AvailableScreeningDto(
        Long screeningId,
        String movieTitle,
        String hallName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal ticketPrice,
        long soldTickets,
        long availableSeats
) {
}
