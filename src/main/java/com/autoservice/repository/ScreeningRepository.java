package com.autoservice.repository;

import com.autoservice.domain.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {
    List<Screening> findAllByOrderByStartTimeAsc();
    List<Screening> findByMovieIdOrderByStartTimeAsc(Long movieId);
    List<Screening> findByHallIdOrderByStartTimeAsc(Long hallId);
    List<Screening> findByStartTimeBetweenOrderByStartTimeAsc(LocalDateTime from, LocalDateTime to);
    List<Screening> findByHallIdAndStartTimeBetweenOrderByStartTimeAsc(Long hallId, LocalDateTime from, LocalDateTime to);
    boolean existsByMovieId(Long movieId);
    boolean existsByHallId(Long hallId);

    @Query("""
            select case when count(s) > 0 then true else false end
            from Screening s
            where s.hall.id = :hallId
              and s.startTime < :endTime
              and s.endTime > :startTime
            """)
    boolean existsOverlappingScreening(@Param("hallId") Long hallId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    @Query("""
            select case when count(s) > 0 then true else false end
            from Screening s
            where s.hall.id = :hallId
              and s.id <> :screeningId
              and s.startTime < :endTime
              and s.endTime > :startTime
            """)
    boolean existsOverlappingScreeningExcludingId(@Param("hallId") Long hallId,
                                                  @Param("screeningId") Long screeningId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
}
