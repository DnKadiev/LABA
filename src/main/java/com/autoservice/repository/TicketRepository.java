package com.autoservice.repository;

import com.autoservice.domain.Ticket;
import com.autoservice.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findAllByOrderByPurchasedAtDesc();
    List<Ticket> findByCustomerIdOrderByPurchasedAtDesc(Long customerId);
    List<Ticket> findByScreeningIdOrderBySeatNumberAsc(Long screeningId);
    long countByScreeningIdAndStatus(Long screeningId, TicketStatus status);
    boolean existsByScreeningId(Long screeningId);
    boolean existsByCustomerId(Long customerId);
    boolean existsByScreeningIdAndSeatNumberAndStatus(Long screeningId, Integer seatNumber, TicketStatus status);
}
