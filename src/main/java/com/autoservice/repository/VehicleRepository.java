package com.autoservice.repository;

import com.autoservice.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByCustomerId(Long customerId);
    boolean existsByLicensePlate(String licensePlate);
    boolean existsByVin(String vin);
}
