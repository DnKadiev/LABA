package com.autoservice.controller;

import com.autoservice.domain.Hall;
import com.autoservice.repository.HallRepository;
import com.autoservice.repository.ScreeningRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/halls")
public class HallController {

    private final HallRepository hallRepository;
    private final ScreeningRepository screeningRepository;

    public HallController(HallRepository hallRepository, ScreeningRepository screeningRepository) {
        this.hallRepository = hallRepository;
        this.screeningRepository = screeningRepository;
    }

    @GetMapping
    public List<Hall> getAll() {
        return hallRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hall> getById(@PathVariable Long id) {
        return hallRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Hall hall) {
        if (hallRepository.existsByName(hall.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Hall name already exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(hallRepository.save(hall));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Hall updated) {
        return hallRepository.findById(id).map(existing -> {
            if (!existing.getName().equals(updated.getName()) && hallRepository.existsByName(updated.getName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Hall name already exists");
            }
            existing.setName(updated.getName());
            existing.setCapacity(updated.getCapacity());
            existing.setPremium(updated.isPremium());
            return ResponseEntity.ok(hallRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!hallRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (screeningRepository.existsByHallId(id)) {
            return ResponseEntity.badRequest().body("Hall cannot be deleted because screenings are linked to it");
        }
        hallRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
