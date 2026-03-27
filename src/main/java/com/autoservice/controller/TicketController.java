package com.autoservice.controller;

import com.autoservice.domain.Ticket;
import com.autoservice.repository.TicketRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;

    public TicketController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @GetMapping
    public List<Ticket> getAll() {
        return ticketRepository.findAllByOrderByPurchasedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getById(@PathVariable Long id) {
        return ticketRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public List<Ticket> getByCustomer(@PathVariable Long customerId) {
        return ticketRepository.findByCustomerIdOrderByPurchasedAtDesc(customerId);
    }

    @GetMapping("/screening/{screeningId}")
    public List<Ticket> getByScreening(@PathVariable Long screeningId) {
        return ticketRepository.findByScreeningIdOrderBySeatNumberAsc(screeningId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!ticketRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        ticketRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
