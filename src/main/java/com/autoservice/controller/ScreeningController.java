package com.autoservice.controller;

import com.autoservice.domain.Hall;
import com.autoservice.domain.Movie;
import com.autoservice.domain.Screening;
import com.autoservice.dto.ScreeningRequest;
import com.autoservice.repository.HallRepository;
import com.autoservice.repository.MovieRepository;
import com.autoservice.repository.ScreeningRepository;
import com.autoservice.repository.TicketRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/screenings")
public class ScreeningController {

    private final ScreeningRepository screeningRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final TicketRepository ticketRepository;

    public ScreeningController(ScreeningRepository screeningRepository,
                               MovieRepository movieRepository,
                               HallRepository hallRepository,
                               TicketRepository ticketRepository) {
        this.screeningRepository = screeningRepository;
        this.movieRepository = movieRepository;
        this.hallRepository = hallRepository;
        this.ticketRepository = ticketRepository;
    }

    @GetMapping
    public List<Screening> getAll() {
        return screeningRepository.findAllByOrderByStartTimeAsc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Screening> getById(@PathVariable Long id) {
        return screeningRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/movie/{movieId}")
    public List<Screening> getByMovie(@PathVariable Long movieId) {
        return screeningRepository.findByMovieIdOrderByStartTimeAsc(movieId);
    }

    @GetMapping("/hall/{hallId}")
    public List<Screening> getByHall(@PathVariable Long hallId) {
        return screeningRepository.findByHallIdOrderByStartTimeAsc(hallId);
    }

    @GetMapping("/date/{date}")
    public List<Screening> getByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return screeningRepository.findByStartTimeBetweenOrderByStartTimeAsc(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ScreeningRequest request) {
        Movie movie = movieRepository.findById(request.movieId()).orElse(null);
        if (movie == null) {
            return ResponseEntity.badRequest().body("Movie not found");
        }
        Hall hall = hallRepository.findById(request.hallId()).orElse(null);
        if (hall == null) {
            return ResponseEntity.badRequest().body("Hall not found");
        }

        LocalDateTime endTime = request.startTime().plusMinutes(movie.getDurationMinutes());
        if (screeningRepository.existsOverlappingScreening(hall.getId(), request.startTime(), endTime)) {
            return ResponseEntity.badRequest().body("The hall already has another screening in this time range");
        }

        Screening screening = new Screening();
        applyRequest(screening, request, movie, hall, endTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(screeningRepository.save(screening));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ScreeningRequest request) {
        Movie movie = movieRepository.findById(request.movieId()).orElse(null);
        if (movie == null) {
            return ResponseEntity.badRequest().body("Movie not found");
        }
        Hall hall = hallRepository.findById(request.hallId()).orElse(null);
        if (hall == null) {
            return ResponseEntity.badRequest().body("Hall not found");
        }

        LocalDateTime endTime = request.startTime().plusMinutes(movie.getDurationMinutes());
        if (screeningRepository.existsOverlappingScreeningExcludingId(hall.getId(), id, request.startTime(), endTime)) {
            return ResponseEntity.badRequest().body("The hall already has another screening in this time range");
        }

        return screeningRepository.findById(id).map(existing -> {
            applyRequest(existing, request, movie, hall, endTime);
            return ResponseEntity.ok(screeningRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!screeningRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (ticketRepository.existsByScreeningId(id)) {
            return ResponseEntity.badRequest().body("Screening cannot be deleted because tickets are linked to it");
        }
        screeningRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applyRequest(Screening screening,
                              ScreeningRequest request,
                              Movie movie,
                              Hall hall,
                              LocalDateTime endTime) {
        screening.setMovie(movie);
        screening.setHall(hall);
        screening.setStartTime(request.startTime());
        screening.setEndTime(endTime);
        screening.setTicketPrice(request.ticketPrice() != null ? request.ticketPrice() : defaultTicketPrice(movie));
        screening.setLanguage(request.language() != null ? request.language() : "RU");
        screening.setFormatType(request.formatType() != null ? request.formatType() : "2D");
    }

    private BigDecimal defaultTicketPrice(Movie movie) {
        return movie.getBaseTicketPrice() != null ? movie.getBaseTicketPrice() : BigDecimal.ZERO;
    }
}
