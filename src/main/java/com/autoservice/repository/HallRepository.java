package com.autoservice.repository;

import com.autoservice.domain.Hall;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HallRepository extends JpaRepository<Hall, Long> {
    boolean existsByName(String name);
}
