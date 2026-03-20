package com.autoservice.repository;

import com.autoservice.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByServiceOrderId(Long serviceOrderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.serviceOrder.id = :orderId AND oi.mandatory = true AND oi.completed = false")
    List<OrderItem> findIncompleteMandatoryItems(@Param("orderId") Long orderId);
}
