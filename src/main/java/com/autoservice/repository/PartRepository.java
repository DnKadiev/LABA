package com.autoservice.repository;

import com.autoservice.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findByPartNumber(String partNumber);
    boolean existsByPartNumber(String partNumber);
}
