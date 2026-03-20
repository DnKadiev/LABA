package com.autoservice.controller;

import com.autoservice.domain.Part;
import com.autoservice.repository.PartRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartRepository partRepository;

    public PartController(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @GetMapping
    public List<Part> getAll() {
        return partRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Part> getById(@PathVariable Long id) {
        return partRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Part part) {
        if (part.getPartNumber() != null && partRepository.existsByPartNumber(part.getPartNumber())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Part number already exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(partRepository.save(part));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Part> update(@PathVariable Long id, @Valid @RequestBody Part updated) {
        return partRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setPartNumber(updated.getPartNumber());
            existing.setPrice(updated.getPrice());
            existing.setStockQuantity(updated.getStockQuantity());
            return ResponseEntity.ok(partRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!partRepository.existsById(id)) return ResponseEntity.notFound().build();
        partRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
