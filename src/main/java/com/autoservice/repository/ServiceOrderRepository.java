package com.autoservice.repository;

import com.autoservice.domain.OrderStatus;
import com.autoservice.domain.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    List<ServiceOrder> findByVehicleId(Long vehicleId);
    List<ServiceOrder> findByMechanicId(Long mechanicId);
    List<ServiceOrder> findByStatus(OrderStatus status);

    @Query("SELECT so FROM ServiceOrder so WHERE so.mechanic.id = :mechanicId AND so.status IN ('OPEN', 'IN_PROGRESS')")
    List<ServiceOrder> findActiveOrdersByMechanic(@Param("mechanicId") Long mechanicId);

    @Query("SELECT COUNT(so) FROM ServiceOrder so WHERE so.mechanic.id = :mechanicId AND so.status IN ('OPEN', 'IN_PROGRESS')")
    long countActiveOrdersByMechanic(@Param("mechanicId") Long mechanicId);
}
