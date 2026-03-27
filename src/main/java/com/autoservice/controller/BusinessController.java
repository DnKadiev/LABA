package com.autoservice.controller;

import com.autoservice.dto.TicketPurchaseRequest;
import com.autoservice.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping("/screenings/{id}/tickets/purchase")
    public ResponseEntity<?> purchaseTicket(@PathVariable Long id, @Valid @RequestBody TicketPurchaseRequest request) {
        try {
            return ResponseEntity.ok(businessService.purchaseTicket(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/tickets/{id}/refund")
    public ResponseEntity<?> refundTicket(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessService.refundTicket(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/screenings/{id}/occupancy")
    public ResponseEntity<?> getScreeningOccupancy(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessService.getScreeningOccupancy(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/halls/{id}/schedule")
    public ResponseEntity<?> getHallSchedule(@PathVariable Long id,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            return ResponseEntity.ok(businessService.getHallSchedule(id, from, to));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/movies/{id}/available-screenings")
    public ResponseEntity<?> getAvailableScreenings(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessService.getAvailableScreeningsForMovie(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
