package com.autoservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record HallScheduleDto(
        Long hallId,
        String hallName,
        int capacity,
        List<HallScheduleItem> screenings
) {
    public record HallScheduleItem(
            Long screeningId,
            String movieTitle,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String language,
            String formatType,
            BigDecimal ticketPrice,
            long soldTickets,
            long availableSeats
    ) {
    }
}
