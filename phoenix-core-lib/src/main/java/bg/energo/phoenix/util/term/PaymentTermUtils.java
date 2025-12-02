package bg.energo.phoenix.util.term;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;

public class PaymentTermUtils {


    /**
     * Checks if the given date is a working day, considering the provided list of weekend days and holidays.
     *
     * @param date     The date to check.
     * @param weekends The list of weekend days.
     * @param holidays The list of holidays.
     * @return {@code true} if the date is a working day, {@code false} otherwise.
     */
    public static boolean isWorkingDay(LocalDate date, List<DayOfWeek> weekends, List<Holiday> holidays) {
        List<LocalDate> holidayDates = holidays.stream().map(Holiday::getHoliday).toList().stream().map(LocalDateTime::toLocalDate).toList();
        return !holidayDates.contains(date) && !weekends.contains(date.getDayOfWeek());
    }

    /**
     * Calculates the deadline date for a given number of days, considering weekends and holidays.
     *
     * @param days     The number of days to add to the end date.
     * @param endDate  The initial end date.
     * @param weekends The list of weekend days.
     * @param holidays The list of holidays.
     * @return The calculated deadline date, taking into account working days.
     * @throws ClientException If there are no working days available.
     */
    public static LocalDate calculateDeadlineForCalendarAndWorkingDays(Integer days, LocalDate endDate, List<DayOfWeek> weekends, List<Holiday> holidays) {
        if (!weekends.isEmpty() && new HashSet<>(weekends).containsAll(List.of(DayOfWeek.values()))) {
            throw new ClientException("cannot calculate payment deadline as no working days are available", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        long termIterator = days;

        while (termIterator != 0) {
            LocalDate nextDay = endDate.plusDays(1);
            if (isWorkingDay(nextDay, weekends, holidays)) {
                termIterator--;
            }
            endDate = nextDay;
        }
        return endDate;
    }

    /**
     * Shifts the given end date to the next or previous working day, based on the provided due date change option.
     *
     * @param endDate       The initial end date to be shifted.
     * @param dueDateChange The due date change option, either "NEXT_WORKING_DAY" or "PREVIOUS_WORKING_DAY".
     * @param weekends      The list of weekend days.
     * @param holidays      The list of holidays.
     * @return The shifted end date, taking into account working days.
     * @throws ClientException If there are no working days available.
     */
    public static LocalDate shiftDateAccordingToTermDueDate(LocalDate endDate, String dueDateChange, List<DayOfWeek> weekends, List<Holiday> holidays) {
        if (!isWorkingDay(endDate, weekends, holidays) && dueDateChange != null) {
            if (new HashSet<>(weekends).containsAll(List.of(DayOfWeek.values()))) {
                throw new ClientException("cannot calculate payment deadline as no working days are available", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
            switch (DueDateChange.valueOf(dueDateChange)) {
                case NEXT_WORKING_DAY -> {
                    while (!isWorkingDay(endDate, weekends, holidays)) {
                        endDate = endDate.plusDays(1);
                    }
                }
                case PREVIOUS_WORKING_DAY -> {
                    while (!isWorkingDay(endDate, weekends, holidays)) {
                        endDate = endDate.minusDays(1);
                    }
                }
            }
        }
        return endDate;
    }

    /**
     * Calculates the end date for a certain number of days, taking into account the last day of the month.
     *
     * @param termValue The number of days to calculate the end date for.
     * @param endDate   The initial end date.
     * @return The calculated end date, taking into account the last day of the month.
     */
    public static LocalDate calculateEndDateForCertainDays(Integer termValue, LocalDate endDate) {
        TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();

        LocalDate certainDay;
        if (termValue >= 1 && termValue <= endDate.lengthOfMonth()) {
            certainDay = endDate.withDayOfMonth(termValue);
            if (certainDay.isBefore(endDate)) {
                certainDay = certainDay.plusMonths(1);
            }
        } else {
            certainDay = endDate.with(adjuster);
        }

        return certainDay;
    }
}
