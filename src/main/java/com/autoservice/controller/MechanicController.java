package com.autoservice.controller;

import com.autoservice.domain.Mechanic;
import com.autoservice.repository.MechanicRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mechanics")
public class MechanicController {

    private final MechanicRepository mechanicRepository;

    public MechanicController(MechanicRepository mechanicRepository) {
        this.mechanicRepository = mechanicRepository;
    }

    @GetMapping
    public List<Mechanic> getAll() {
        return mechanicRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mechanic> getById(@PathVariable Long id) {
        return mechanicRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public List<Mechanic> getActive() {
        return mechanicRepository.findByActiveTrue();
    }

    @PostMapping
    public ResponseEntity<Mechanic> create(@Valid @RequestBody Mechanic mechanic) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mechanicRepository.save(mechanic));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Mechanic> update(@PathVariable Long id, @Valid @RequestBody Mechanic updated) {
        return mechanicRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setSpecialization(updated.getSpecialization());
            existing.setActive(updated.isActive());
            return ResponseEntity.ok(mechanicRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!mechanicRepository.existsById(id)) return ResponseEntity.notFound().build();
        mechanicRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
