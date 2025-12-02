package bg.energo.phoenix.util.epb;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Slf4j
public class EPBDateUtils {

    /**
     * Checks if the given date is in the given range.
     *
     * @param dateToCheck the date to check
     * @param startDate   the start date of the range
     * @param endDate     the end date of the range
     * @return true if the date is in the range, false otherwise
     */
    public static Boolean isDateInRange(LocalDate dateToCheck, LocalDate startDate, LocalDate endDate) {
        return !dateToCheck.isBefore(startDate) && !dateToCheck.isAfter(endDate);
    }


    /**
     * Checks if the given value is a valid local date.
     *
     * @param value     the value to check
     * @param formatter the formatter to use
     * @return true if the value is a valid local date, false otherwise
     */
    public static boolean isValidLocalDate(String value, DateTimeFormatter formatter) {
        try {
            LocalDate.parse(value, formatter);
            return true;
        } catch (DateTimeParseException e) {
            log.error("Error parsing date: {}", value);
            return false;
        }
    }


    /**
     * Checks if the given value is a valid local date time.
     *
     * @param value     the value to check
     * @param formatter the formatter to use
     * @return true if the value is a valid local date time, false otherwise
     */
    public static boolean isValidLocalDateTime(String value, DateTimeFormatter formatter) {
        try {
            LocalDateTime.parse(value, formatter);
            return true;
        } catch (DateTimeParseException e) {
            log.error("Error parsing date: {}", value);
            return false;
        }
    }


    /**
     * @return months between two dates. If period is shorter than one month, returns 0.
     * If period is longer than whole month, returns the number of whole months.
     */
    public static long calculateMonthsBetween(LocalDate startDate, LocalDate endDate) {
        long months = ChronoUnit.MONTHS.between(startDate, endDate);
        return Math.max(months, 0);
    }


    /**
     * @return days between two dates. If period is shorter than one day, returns 0.
     * If period is longer than whole day, returns the number of whole days.
     */
    public static long calculateDaysBetween(LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return Math.max(days, 0);
    }

    /**
     * Returns the minimum of two LocalDate objects.
     *
     * @param date1 the first LocalDate object to compare
     * @param date2 the second LocalDate object to compare
     * @return the minimum of the two LocalDate objects, or null if both are null
     */
    public static LocalDate min(LocalDate date1, LocalDate date2) {
        if (date1 == null && date2 == null) {
            return null;
        } else if (date1 == null) {
            return date2;
        } else if (date2 == null) {
            return date1;
        }

        if (date1.isBefore(date2)) {
            return date1;
        } else {
            return date2;
        }
    }

    /**
     * Returns the maximum of two LocalDate instances.
     *
     * @param date1 the first LocalDate instance to compare
     * @param date2 the second LocalDate instance to compare
     * @return the maximum of the two LocalDate instances, or null if both are null
     */
    public static LocalDate max(LocalDate date1, LocalDate date2) {
        if (date1 == null && date2 == null) {
            return null;
        } else if (date1 == null) {
            return date2;
        } else if (date2 == null) {
            return date1;
        }

        if (date1.isAfter(date2)) {
            return date1;
        } else {
            return date2;
        }
    }
}
