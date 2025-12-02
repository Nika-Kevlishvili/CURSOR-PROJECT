package bg.energo.phoenix.repository.nomenclature.product.terms;

import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HolidaysRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findAllByCalendarId(Long calendarId);

    @Query("""
            select h from Holiday h
            where h.calendarId = :calendarId
            and h.holidayStatus in (:statuses)
            """)
    List<Holiday> findAllByCalendarIdAndHolidayStatus(@Param("calendarId") Long calendarId, @Param("statuses") List<HolidayStatus> statuses);

    @Query("""
            select h from Holiday h
            where h.calendarId = :calendarId
            and h.holidayStatus in (:statuses)
            and h.holiday = :holidayDate
            """)
    List<Holiday> findAllByCalendarIdAndHolidayStatusAndHolidayDate(@Param("calendarId") Long calendarId, @Param("statuses") List<HolidayStatus> statuses, @Param("holidayDate") LocalDateTime holidayDate);

    @Query("""
            select h from Holiday h
            where h.id = :id
            and h.holidayStatus in(:statuses)
            """)
    Optional<Holiday> findByIdAndStatus(@Param("id") Long id, @Param("statuses") List<HolidayStatus> statuses);
}
