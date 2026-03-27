package com.autoservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScreeningRequest(
        @NotNull Long movieId,
        @NotNull Long hallId,
        @NotNull @Future LocalDateTime startTime,
        @DecimalMin("0.0") BigDecimal ticketPrice,
        String language,
        String formatType
) {
}
