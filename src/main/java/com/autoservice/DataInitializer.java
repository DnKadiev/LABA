package com.autoservice;

import com.autoservice.domain.Customer;
import com.autoservice.domain.Hall;
import com.autoservice.domain.Movie;
import com.autoservice.domain.Screening;
import com.autoservice.domain.Ticket;
import com.autoservice.domain.TicketStatus;
import com.autoservice.repository.CustomerRepository;
import com.autoservice.repository.HallRepository;
import com.autoservice.repository.MovieRepository;
import com.autoservice.repository.ScreeningRepository;
import com.autoservice.repository.TicketRepository;
import com.autoservice.security.AppUser;
import com.autoservice.security.AppUserRepository;
import com.autoservice.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CustomerRepository customerRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final ScreeningRepository screeningRepository;
    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CustomerRepository customerRepository,
                           MovieRepository movieRepository,
                           HallRepository hallRepository,
                           ScreeningRepository screeningRepository,
                           TicketRepository ticketRepository,
                           AppUserRepository appUserRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.movieRepository = movieRepository;
        this.hallRepository = hallRepository;
        this.screeningRepository = screeningRepository;
        this.ticketRepository = ticketRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            log.info("Database already seeded, skipping initialization.");
            return;
        }

        log.info("Seeding database with initial cinema booking data...");

        createUserIfAbsent("admin", "Admin1234!", Role.ROLE_ADMIN);
        createUserIfAbsent("manager1", "Cinema1234!", Role.ROLE_MANAGER);
        createUserIfAbsent("customer1", "Cust1234!", Role.ROLE_CUSTOMER);

        Customer belova = createCustomer("Мария Белова", "+7-900-100-0001", "belova@example.com");
        Customer smirnov = createCustomer("Денис Смирнов", "+7-900-100-0002", "smirnov@example.com");
        Customer volkova = createCustomer("Елена Волкова", "+7-900-100-0003", "volkova@example.com");

        Movie dune = createMovie(
                "Dune: Part Two",
                "Sci-Fi",
                166,
                "16+",
                "Пол Атрейдес объединяется с фременами, чтобы отомстить заговорщикам.",
                LocalDate.of(2024, 2, 29),
                new BigDecimal("650.00"),
                true
        );
        Movie interstellar = createMovie(
                "Interstellar",
                "Sci-Fi",
                169,
                "12+",
                "Экспедиция через кротовую нору в поисках нового дома для человечества.",
                LocalDate.of(2014, 11, 6),
                new BigDecimal("500.00"),
                true
        );
        Movie spiritedAway = createMovie(
                "Spirited Away",
                "Animation",
                125,
                "6+",
                "Приключение Тихиро в волшебном мире духов.",
                LocalDate.of(2001, 7, 20),
                new BigDecimal("420.00"),
                true
        );

        Hall redHall = createHall("Red Hall", 120, false);
        Hall blueHall = createHall("Blue Hall", 80, false);
        Hall vipHall = createHall("VIP Hall", 36, true);

        LocalDateTime now = LocalDateTime.now();

        Screening duneEvening = createScreening(dune, redHall,
                now.plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0),
                new BigDecimal("690.00"), "RU", "IMAX");
        Screening interstellarNight = createScreening(interstellar, blueHall,
                now.plusDays(2).withHour(21).withMinute(30).withSecond(0).withNano(0),
                new BigDecimal("540.00"), "EN", "2D");
        Screening spiritedFamily = createScreening(spiritedAway, vipHall,
                now.plusDays(3).withHour(13).withMinute(0).withSecond(0).withNano(0),
                new BigDecimal("470.00"), "RU", "2D");
        Screening duneMorning = createScreening(dune, blueHall,
                now.plusHours(8).withMinute(0).withSecond(0).withNano(0),
                new BigDecimal("620.00"), "RU", "2D");
        Screening archiveScreening = createScreening(interstellar, redHall,
                now.minusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0),
                new BigDecimal("500.00"), "EN", "2D");

        createTicket(duneEvening, belova, 1, new BigDecimal("690.00"), "TCK-DUNE001", TicketStatus.PURCHASED);
        createTicket(duneEvening, smirnov, 2, new BigDecimal("690.00"), "TCK-DUNE002", TicketStatus.PURCHASED);
        createTicket(duneEvening, volkova, 3, new BigDecimal("690.00"), "TCK-DUNE003", TicketStatus.REFUNDED);
        createTicket(interstellarNight, belova, 5, new BigDecimal("540.00"), "TCK-INT001", TicketStatus.PURCHASED);
        createTicket(spiritedFamily, volkova, 1, new BigDecimal("470.00"), "TCK-SPR001", TicketStatus.PURCHASED);
        createTicket(duneMorning, smirnov, 4, new BigDecimal("620.00"), "TCK-DUNEM01", TicketStatus.PURCHASED);
        createTicket(archiveScreening, belova, 10, new BigDecimal("500.00"), "TCK-OLD001", TicketStatus.PURCHASED);

        log.info("Database seeded: {} customers, {} movies, {} halls, {} screenings, {} tickets.",
                customerRepository.count(),
                movieRepository.count(),
                hallRepository.count(),
                screeningRepository.count(),
                ticketRepository.count());
    }

    private void createUserIfAbsent(String username, String rawPassword, Role role) {
        if (!appUserRepository.existsByUsername(username)) {
            AppUser user = new AppUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            appUserRepository.save(user);
        }
    }

    private Customer createCustomer(String fullName, String phone, String email) {
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setEmail(email);
        return customerRepository.save(customer);
    }

    private Movie createMovie(String title,
                              String genre,
                              int durationMinutes,
                              String ageRating,
                              String description,
                              LocalDate releaseDate,
                              BigDecimal baseTicketPrice,
                              boolean active) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setDurationMinutes(durationMinutes);
        movie.setAgeRating(ageRating);
        movie.setDescription(description);
        movie.setReleaseDate(releaseDate);
        movie.setBaseTicketPrice(baseTicketPrice);
        movie.setActive(active);
        return movieRepository.save(movie);
    }

    private Hall createHall(String name, int capacity, boolean premium) {
        Hall hall = new Hall();
        hall.setName(name);
        hall.setCapacity(capacity);
        hall.setPremium(premium);
        return hallRepository.save(hall);
    }

    private Screening createScreening(Movie movie,
                                      Hall hall,
                                      LocalDateTime startTime,
                                      BigDecimal ticketPrice,
                                      String language,
                                      String formatType) {
        Screening screening = new Screening();
        screening.setMovie(movie);
        screening.setHall(hall);
        screening.setStartTime(startTime);
        screening.setEndTime(startTime.plusMinutes(movie.getDurationMinutes()));
        screening.setTicketPrice(ticketPrice);
        screening.setLanguage(language);
        screening.setFormatType(formatType);
        return screeningRepository.save(screening);
    }

    private Ticket createTicket(Screening screening,
                                Customer customer,
                                int seatNumber,
                                BigDecimal paidPrice,
                                String bookingCode,
                                TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setScreening(screening);
        ticket.setCustomer(customer);
        ticket.setSeatNumber(seatNumber);
        ticket.setPaidPrice(paidPrice);
        ticket.setBookingCode(bookingCode);
        ticket.setStatus(status);
        if (status == TicketStatus.REFUNDED) {
            ticket.setRefundedAt(LocalDateTime.now().minusHours(2));
        }
        return ticketRepository.save(ticket);
    }
}
