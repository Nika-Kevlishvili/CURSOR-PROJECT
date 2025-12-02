package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.BillingInvoicePaymentTerm;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentTerms;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.HolidaysRepository;
import bg.energo.phoenix.util.term.PaymentTermUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class BillingPaymentTermDayCalculationService {

    private final CalendarRepository calendarRepository;
    private final HolidaysRepository holidaysRepository;


    public LocalDate calculateDeadline(BillingInvoicePaymentTerm term, LocalDate invoiceDate, Integer value) {
        LocalDate endDate = invoiceDate;

        DueDateChange dueDateChange = term.getDueDateChange();

        Long calendarId = term.getCalendarId();
        Calendar calendar = calendarRepository
                .findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Presented Payment Term calendar not found;"));
        List<DayOfWeek> weekends = Arrays.stream(
                        Objects.requireNonNullElse(calendar.getWeekends(), "")
                                .split(";")
                )
                .filter(StringUtils::isNotBlank)
                .map(DayOfWeek::valueOf)
                .toList();
        List<Holiday> holidays = holidaysRepository.findAllByCalendarIdAndHolidayStatus(calendarId, List.of(HolidayStatus.ACTIVE));

        switch (term.getCalendarType()) {
            case CALENDAR_DAYS -> {
                weekends = term.getExcludeWeekends() ? weekends : new ArrayList<>();
                holidays = term.getExcludeHolidays() ? holidays : new ArrayList<>();
                endDate = calculateDeadlineForCalendarDays(value, endDate);
                endDate = PaymentTermUtils.shiftDateAccordingToTermDueDate(endDate, Objects.nonNull(dueDateChange) ? dueDateChange.name() : null, weekends, holidays);
            }
            case WORKING_DAYS ->
                    endDate = PaymentTermUtils.calculateDeadlineForCalendarAndWorkingDays(value, endDate, weekends, holidays);
            case CERTAIN_DAYS -> {
                LocalDate certainDay = LocalDate.of(endDate.getYear(), endDate.getMonthValue(), value);
                endDate = certainDay.isBefore(endDate) ? certainDay.plusMonths(1) : certainDay;
                endDate = PaymentTermUtils.shiftDateAccordingToTermDueDate(endDate, Objects.nonNull(dueDateChange) ? dueDateChange.name() : null, weekends, holidays);
            }
        }
        return endDate;
    }

    public LocalDate calculateDeadline(InterimAdvancePaymentTerms term, LocalDate invoiceDate, Integer value) {
        LocalDate endDate = invoiceDate;

        DueDateChange dueDateChange = term.getDueDateChange();

        Long calendarId = term.getCalendar().getId();
        Calendar calendar = calendarRepository
                .findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Presented Payment Term calendar not found;"));
        List<DayOfWeek> weekends = Arrays.stream(
                        Objects.requireNonNullElse(calendar.getWeekends(), "")
                                .split(";")
                )
                .filter(StringUtils::isNotBlank)
                .map(DayOfWeek::valueOf)
                .toList();
        List<Holiday> holidays = holidaysRepository.findAllByCalendarIdAndHolidayStatus(calendarId, List.of(HolidayStatus.ACTIVE));

        switch (term.getCalendarType()) {
            case CALENDAR_DAYS -> {
                weekends = term.getExcludeWeekends() ? weekends : new ArrayList<>();
                holidays = term.getExcludeHolidays() ? holidays : new ArrayList<>();
                endDate = calculateDeadlineForCalendarDays(value, endDate);
                endDate = calculateEndDateByAccordingToTermDueDate(endDate, dueDateChange, weekends, holidays);
            }
            case WORKING_DAYS ->
                    endDate = PaymentTermUtils.calculateDeadlineForCalendarAndWorkingDays(value, endDate, weekends, holidays);
            case CERTAIN_DAYS -> {
                LocalDate certainDay = LocalDate.of(endDate.getYear(), endDate.getMonthValue(), value);
                endDate = certainDay.isBefore(endDate) ? certainDay.plusMonths(1) : certainDay;
                endDate = calculateEndDateByAccordingToTermDueDate(endDate, dueDateChange, weekends, holidays);
            }
        }
        return endDate;
    }


    private LocalDate calculateDeadlineForCalendarDays(Integer days, LocalDate endDate) {
        return endDate.plusDays(days);
    }

    private LocalDate calculateEndDateByAccordingToTermDueDate(LocalDate endDate, DueDateChange dueDateChange, List<DayOfWeek> weekends, List<Holiday> holidays) {
        if (!PaymentTermUtils.isWorkingDay(endDate, weekends, holidays) && dueDateChange != null) {
            if (new HashSet<>(weekends).containsAll(List.of(DayOfWeek.values()))) {
                throw new ClientException("cannot calculate payment deadline as no working days are available", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
            switch (dueDateChange) {
                case NEXT_WORKING_DAY -> {
                    while (!PaymentTermUtils.isWorkingDay(endDate, weekends, holidays)) {
                        endDate = endDate.plusDays(1);
                    }
                }
                case PREVIOUS_WORKING_DAY -> {
                    while (!PaymentTermUtils.isWorkingDay(endDate, weekends, holidays)) {
                        endDate = endDate.minusDays(1);
                    }
                }
            }
        }

        return endDate;
    }
}
