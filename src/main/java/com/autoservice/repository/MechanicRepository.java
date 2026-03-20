package com.autoservice.repository;

import com.autoservice.domain.Mechanic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MechanicRepository extends JpaRepository<Mechanic, Long> {
    List<Mechanic> findByActiveTrue();

    @Query("SELECT m FROM Mechanic m WHERE m.active = true ORDER BY (SELECT COUNT(so) FROM ServiceOrder so WHERE so.mechanic = m AND so.status IN ('OPEN', 'IN_PROGRESS')) ASC")
    List<Mechanic> findActiveMechanicsByWorkloadAsc();
}
