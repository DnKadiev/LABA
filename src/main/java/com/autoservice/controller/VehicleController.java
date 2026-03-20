package com.autoservice.controller;

import com.autoservice.domain.Customer;
import com.autoservice.domain.Vehicle;
import com.autoservice.repository.CustomerRepository;
import com.autoservice.repository.VehicleRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;

    public VehicleController(VehicleRepository vehicleRepository, CustomerRepository customerRepository) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<Vehicle> getAll() {
        return vehicleRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getById(@PathVariable Long id) {
        return vehicleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public List<Vehicle> getByCustomer(@PathVariable Long customerId) {
        return vehicleRepository.findByCustomerId(customerId);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Vehicle vehicle) {
        if (vehicle.getLicensePlate() != null && vehicleRepository.existsByLicensePlate(vehicle.getLicensePlate())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("License plate already exists");
        }
        if (vehicle.getVin() != null && vehicleRepository.existsByVin(vehicle.getVin())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("VIN already exists");
        }
        if (vehicle.getCustomer() == null || vehicle.getCustomer().getId() == null) {
            return ResponseEntity.badRequest().body("Customer ID is required");
        }
        Customer customer = customerRepository.findById(vehicle.getCustomer().getId())
                .orElse(null);
        if (customer == null) return ResponseEntity.badRequest().body("Customer not found");
        vehicle.setCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleRepository.save(vehicle));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Vehicle updated) {
        return vehicleRepository.findById(id).map(existing -> {
            existing.setMake(updated.getMake());
            existing.setModel(updated.getModel());
            existing.setYear(updated.getYear());
            existing.setLicensePlate(updated.getLicensePlate());
            existing.setVin(updated.getVin());
            return ResponseEntity.ok(vehicleRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!vehicleRepository.existsById(id)) return ResponseEntity.notFound().build();
        vehicleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
