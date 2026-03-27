package com.autoservice.service;

import com.autoservice.domain.Customer;
import com.autoservice.domain.Hall;
import com.autoservice.domain.Movie;
import com.autoservice.domain.Screening;
import com.autoservice.domain.Ticket;
import com.autoservice.domain.TicketStatus;
import com.autoservice.dto.AvailableScreeningDto;
import com.autoservice.dto.HallScheduleDto;
import com.autoservice.dto.ScreeningOccupancyDto;
import com.autoservice.dto.TicketPurchaseRequest;
import com.autoservice.repository.CustomerRepository;
import com.autoservice.repository.HallRepository;
import com.autoservice.repository.MovieRepository;
import com.autoservice.repository.ScreeningRepository;
import com.autoservice.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessService {

    private final ScreeningRepository screeningRepository;
    private final TicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final HallRepository hallRepository;
    private final MovieRepository movieRepository;

    public BusinessService(ScreeningRepository screeningRepository,
                           TicketRepository ticketRepository,
                           CustomerRepository customerRepository,
                           HallRepository hallRepository,
                           MovieRepository movieRepository) {
        this.screeningRepository = screeningRepository;
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.hallRepository = hallRepository;
        this.movieRepository = movieRepository;
    }

    @Transactional
    public Ticket purchaseTicket(Long screeningId, TicketPurchaseRequest request) {
        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found: " + screeningId));
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        if (!LocalDateTime.now().isBefore(screening.getStartTime())) {
            throw new IllegalStateException("Ticket purchase is available only before the screening starts");
        }

        int hallCapacity = screening.getHall().getCapacity();
        if (request.seatNumber() > hallCapacity) {
            throw new IllegalStateException("Seat number exceeds hall capacity");
        }

        long soldTickets = ticketRepository.countByScreeningIdAndStatus(screeningId, TicketStatus.PURCHASED);
        if (soldTickets >= hallCapacity) {
            throw new IllegalStateException("Hall capacity exceeded for this screening");
        }

        if (ticketRepository.existsByScreeningIdAndSeatNumberAndStatus(screeningId, request.seatNumber(), TicketStatus.PURCHASED)) {
            throw new IllegalStateException("Seat " + request.seatNumber() + " is already occupied");
        }

        Ticket ticket = new Ticket();
        ticket.setScreening(screening);
        ticket.setCustomer(customer);
        ticket.setSeatNumber(request.seatNumber());
        ticket.setPaidPrice(request.paidPrice() != null ? request.paidPrice() : screening.getTicketPrice());
        ticket.setBookingCode("TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ticket.setStatus(TicketStatus.PURCHASED);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket refundTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new IllegalStateException("Ticket already refunded");
        }
        if (!LocalDateTime.now().isBefore(ticket.getScreening().getStartTime())) {
            throw new IllegalStateException("Refund is available only before the screening starts");
        }

        ticket.setStatus(TicketStatus.REFUNDED);
        ticket.setRefundedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public ScreeningOccupancyDto getScreeningOccupancy(Long screeningId) {
        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found: " + screeningId));
        long soldTickets = ticketRepository.countByScreeningIdAndStatus(screeningId, TicketStatus.PURCHASED);
        long refundedTickets = ticketRepository.countByScreeningIdAndStatus(screeningId, TicketStatus.REFUNDED);
        int hallCapacity = screening.getHall().getCapacity();
        long availableSeats = hallCapacity - soldTickets;
        BigDecimal occupancyPercent = BigDecimal.valueOf(soldTickets)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(hallCapacity), 2, RoundingMode.HALF_UP);

        return new ScreeningOccupancyDto(
                screening.getId(),
                screening.getMovie().getTitle(),
                screening.getHall().getName(),
                screening.getStartTime(),
                hallCapacity,
                soldTickets,
                refundedTickets,
                availableSeats,
                occupancyPercent
        );
    }

    @Transactional(readOnly = true)
    public HallScheduleDto getHallSchedule(Long hallId, LocalDate from, LocalDate to) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new IllegalArgumentException("Hall not found: " + hallId));
        LocalDate fromDate = from != null ? from : LocalDate.now();
        LocalDate toDate = to != null ? to : fromDate.plusDays(7);
        if (toDate.isBefore(fromDate)) {
            throw new IllegalStateException("The end date must be greater than or equal to the start date");
        }

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.plusDays(1).atStartOfDay();

        List<HallScheduleDto.HallScheduleItem> screenings = screeningRepository
                .findByHallIdAndStartTimeBetweenOrderByStartTimeAsc(hallId, fromDateTime, toDateTime)
                .stream()
                .map(screening -> {
                    long soldTickets = ticketRepository.countByScreeningIdAndStatus(screening.getId(), TicketStatus.PURCHASED);
                    long availableSeats = screening.getHall().getCapacity() - soldTickets;
                    return new HallScheduleDto.HallScheduleItem(
                            screening.getId(),
                            screening.getMovie().getTitle(),
                            screening.getStartTime(),
                            screening.getEndTime(),
                            screening.getLanguage(),
                            screening.getFormatType(),
                            screening.getTicketPrice(),
                            soldTickets,
                            availableSeats
                    );
                })
                .toList();

        return new HallScheduleDto(hall.getId(), hall.getName(), hall.getCapacity(), screenings);
    }

    @Transactional(readOnly = true)
    public List<AvailableScreeningDto> getAvailableScreeningsForMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + movieId));

        return screeningRepository.findByMovieIdOrderByStartTimeAsc(movie.getId())
                .stream()
                .filter(screening -> screening.getStartTime().isAfter(LocalDateTime.now()))
                .map(screening -> {
                    long soldTickets = ticketRepository.countByScreeningIdAndStatus(screening.getId(), TicketStatus.PURCHASED);
                    long availableSeats = screening.getHall().getCapacity() - soldTickets;
                    return new AvailableScreeningDto(
                            screening.getId(),
                            screening.getMovie().getTitle(),
                            screening.getHall().getName(),
                            screening.getStartTime(),
                            screening.getEndTime(),
                            screening.getTicketPrice(),
                            soldTickets,
                            availableSeats
                    );
                })
                .filter(screening -> screening.availableSeats() > 0)
                .toList();
    }
}
