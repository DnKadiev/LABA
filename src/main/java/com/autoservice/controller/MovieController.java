package com.autoservice.controller;

import com.autoservice.domain.Movie;
import com.autoservice.repository.MovieRepository;
import com.autoservice.repository.ScreeningRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;

    public MovieController(MovieRepository movieRepository, ScreeningRepository screeningRepository) {
        this.movieRepository = movieRepository;
        this.screeningRepository = screeningRepository;
    }

    @GetMapping
    public List<Movie> getAll() {
        return movieRepository.findAll();
    }

    @GetMapping("/active")
    public List<Movie> getActive() {
        return movieRepository.findByActiveTrue();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getById(@PathVariable Long id) {
        return movieRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Movie> create(@Valid @RequestBody Movie movie) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movieRepository.save(movie));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Movie> update(@PathVariable Long id, @Valid @RequestBody Movie updated) {
        return movieRepository.findById(id).map(existing -> {
            existing.setTitle(updated.getTitle());
            existing.setGenre(updated.getGenre());
            existing.setDurationMinutes(updated.getDurationMinutes());
            existing.setAgeRating(updated.getAgeRating());
            existing.setDescription(updated.getDescription());
            existing.setReleaseDate(updated.getReleaseDate());
            existing.setBaseTicketPrice(updated.getBaseTicketPrice());
            existing.setActive(updated.isActive());
            return ResponseEntity.ok(movieRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!movieRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (screeningRepository.existsByMovieId(id)) {
            return ResponseEntity.badRequest().body("Movie cannot be deleted because screenings are linked to it");
        }
        movieRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
