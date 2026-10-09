package bg.energo.phoenix.utils;

import bg.energo.phoenix.model.CheckLiabilityDetails;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class EPBFunctionUtils {

    /**
     * Payment-source AIDs (last 6 symbols of TID) for which pro-forma reconnection
     * liabilities must be hidden from EasyPay BILLING responses.
     */
    public static final Set<String> PROFORMA_RECONNECTION_EXCLUDED_AIDS = Set.of(
            "700020", "700021", "700022", "700023", "700024",
            "700025", "700026", "700027", "700028", "700029"
    );

    /**
     * Converts a LocalDateTime object into a formatted string.
     * The format used is "yyyyMMddHHmmss" (year, month, day, hour, minute, second).
     *
     * @param localDateTime The LocalDateTime object to be converted to a string.
     * @return The formatted string representing the LocalDateTime.
     */
    public static String dateStringFromLocalDateTime(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return localDateTime.format(formatter);
    }

    /**
     * Converts a LocalDate object into a formatted string.
     * The format used is "yyyyMMddHHmmss", where the time portion is set to the start of the day (00:00:00).
     *
     * @param localDate The LocalDate object to be converted to a string.
     * @return The formatted string representing the LocalDate at the start of the day.
     */
    public static String dateStringFromLocalDate(LocalDate localDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return localDate.atStartOfDay().format(formatter);
    }

    /**
     * Validates a transaction ID string. The expected structure of the transaction ID is as follows:
     * - The first 14 characters should represent a valid date-time in the "yyyyMMddHHmmss" format.
     * - The next 6 characters represent a valid 6-digit STAN (System Trace Audit Number).
     * - The last 6 characters represent a valid AID (Acquirer Identification).
     * The AID must start with either "70002" or "7001".
     *
     * @param transactionStr The transaction ID string to be validated.
     * @return true if the transaction ID string is valid, false otherwise.
     */
    public static boolean validateTransactionId(String transactionStr) {
        log.debug("Received transaction id: {}", transactionStr);

        // Check if the total length of the transaction string is 26 characters
        if (transactionStr.length() != 26) {
            log.error("Invalid length: Expected 26 characters, found {}", transactionStr.length());
            return false;
        }

        // Extract DATE (first 14 characters)
        String dateStr = transactionStr.substring(0, 14);
        log.debug("Extracted DATE: {}", dateStr);

        // Validate DATE (14 characters)
        if (!dateStr.matches("\\d+")) {
            log.error("Invalid DATE format: {}", dateStr);
            return false;
        }

        // Validate if DATE is a valid date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setLenient(false); // Disable lenient parsing
        try {
            sdf.parse(dateStr); // Try parsing the date
            log.debug("DATE is valid: {}", dateStr);
        } catch (Exception e) {
            log.error("Invalid DATE value: {}", dateStr);
            return false;
        }

        // Extract STAN (next 6 characters)
        String stanStr = transactionStr.substring(14, 20);
        log.debug("Extracted STAN: {}", stanStr);

        // Validate STAN (6 characters)
        if (!stanStr.matches("\\d{6}")) {
            log.error("Invalid STAN format: {}", stanStr);
            return false;
        }
        log.debug("STAN is valid: {}", stanStr);

        // Extract AID (last 6 characters)
        String aidStr = transactionStr.substring(20, 26);
        log.debug("Extracted AID: {}", aidStr);

        // Validate AID (6 characters)
        if (!aidStr.matches("\\d{6}")) {
            log.error("Invalid AID format: {}", aidStr);
            return false;
        }

        log.debug("AID is valid: {}", aidStr);

        // If all validations pass, return success
        return true;
    }

    /**
     * Converts a date string in the format "yyyyMMddHHmmss" to a LocalDateTime object.
     *
     * @param dateString The date string to be parsed.
     * @return A LocalDateTime object representing the parsed date and time.
     *         Returns null if the parsing fails.
     */
    public static LocalDateTime localDateTimeFromDateString(String dateString) {
        // Define the format for parsing
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        LocalDateTime dateTime = null;

        // Log input date string at DEBUG level
        log.debug("Attempting to parse date: {}", dateString);

        try {
            // Parse the string into LocalDateTime
            dateTime = LocalDateTime.parse(dateString, formatter);

            log.info("Successfully parsed date: Input String = '{}' | Parsed LocalDateTime = '{}'", dateString, dateTime);
        } catch (Exception e) {
            log.error("Error parsing date. Invalid format. Expected format: yyyyMMddHHmmss.");
        }

        return dateTime;
    }

    /**
     * Number of calendar days added to the current date when a liability has no due date
     * (proforma invoices). Used for EasyPay {@code VALIDTO}.
     */
    public static final int PROFORMA_VALID_TO_DAYS = 30;

    /**
     * Resolves the EasyPay VALIDTO date for a liability.
     * When {@code dueDate} is present it is used as-is; when null (proforma invoices)
     * returns {@code currentDate + PROFORMA_VALID_TO_DAYS}.
     *
     * @param dueDate     liability due date, may be null for proforma invoices
     * @param currentDate reference date (typically {@link LocalDate#now()})
     * @return date to send as VALIDTO
     */
    public static LocalDate resolveValidToDate(LocalDate dueDate, LocalDate currentDate) {
        return dueDate != null ? dueDate : currentDate.plusDays(PROFORMA_VALID_TO_DAYS);
    }

    /**
     * Returns whether pro-forma reconnection liabilities must be excluded for this TID.
     * Uses the last 6 symbols of the TID (AID / payment source).
     *
     * @param tid transaction ID from an EasyPay BILLING request
     * @return true when the TID ends with one of {@link #PROFORMA_RECONNECTION_EXCLUDED_AIDS}
     */
    public static boolean shouldHideProformaReconnectionLiabilities(String tid) {
        if (StringUtils.isBlank(tid) || tid.length() < 6) {
            return false;
        }
        String aid = tid.substring(tid.length() - 6);
        return PROFORMA_RECONNECTION_EXCLUDED_AIDS.contains(aid);
    }

    /**
     * Removes liabilities whose IDs are in {@code idsToExclude}.
     * Returns the original list when there is nothing to exclude.
     */
    public static List<CheckLiabilityDetails> excludeLiabilitiesByIds(
            List<CheckLiabilityDetails> liabilities,
            Set<Long> idsToExclude
    ) {
        if (liabilities == null || liabilities.isEmpty()
                || idsToExclude == null || idsToExclude.isEmpty()) {
            return liabilities;
        }
        return liabilities.stream()
                .filter(liability -> !idsToExclude.contains(liability.getLiabilityId()))
                .toList();
    }

    /**
     * Finds the nearest due date from a list of CheckLiabilityDetails.
     * The due date is compared with the provided current date and the closest one is returned.
     * When all liabilities lack a due date (e.g. only proforma invoices), returns
     * {@code currentDate + PROFORMA_VALID_TO_DAYS}.
     *
     * @param liabilities The list of CheckLiabilityDetails containing due dates to be checked.
     * @param currentDate The current date used for comparison.
     * @return The nearest due date from the liabilities list, or today+30 when none have a due date.
     */
    public static LocalDate getNearestDueDate(List<CheckLiabilityDetails> liabilities, LocalDate currentDate) {
        Optional<CheckLiabilityDetails> nearestLiability = liabilities
                .stream()
                .filter(liability -> liability.getDueDate() != null)
                .min((liabilityOne, liabilityTwo) -> {
                            long diff1 = Math.abs(liabilityOne.getDueDate().toEpochDay() - currentDate.toEpochDay());
                            long diff2 = Math.abs(liabilityTwo.getDueDate().toEpochDay() - currentDate.toEpochDay());
                            return Long.compare(diff1, diff2);
                        }
                );

        return nearestLiability
                .map(CheckLiabilityDetails::getDueDate)
                .orElseGet(() -> currentDate.plusDays(PROFORMA_VALID_TO_DAYS));
    }

}
