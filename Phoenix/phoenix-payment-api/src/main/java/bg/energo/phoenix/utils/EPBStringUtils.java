package bg.energo.phoenix.utils;

import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.response.customer.CustomerPaymentInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class for formatting strings with optional line breaks.
 */
@Slf4j
public class EPBStringUtils {

    /**
     * Inserts line breaks into the provided text after every specified number of characters.
     *
     * @param text          the text to format with line breaks
     * @param maxLineLength the maximum length of each line before inserting a line break
     * @return the formatted string with line breaks inserted
     */
    public static String insertLineBreaks(String text, int maxLineLength) {
        StringBuilder sb = new StringBuilder(text.length() + (text.length() / maxLineLength));
        int length = text.length();

        for (int i = 0; i < length; i += maxLineLength) {
            // Determine the end of the current line
            int end = Math.min(length, i + maxLineLength);
            sb.append(text, i, end).append('\n');
        }

        // Remove the last newline if unnecessary
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * Formats a short description with customer number.
     * Format: "Клиентски номер: {CUSTOMER NUMBER};"
     * Maximum length: 40 characters (truncated if longer).
     *
     * @param customerNumber the customer number
     * @return the formatted short description (max 40 chars)
     */
    public static String formatShortDesc(String customerNumber) {
        String combinedString = String.format(
                "Клиентски номер: %s;",
                StringUtils.isBlank(customerNumber) ? "" : customerNumber
        );

        // Truncate to 40 characters if longer
        if (combinedString.length() > 40) {
            return combinedString.substring(0, 40);
        }
        return combinedString;
    }

    /**
     * Formats a short description including the liability number.
     *
     * @param liabilityNumber the liability number
     * @return the formatted short description with line breaks inserted
     */
    public static String formatInvoiceShortDesc(String customerNumber, String liabilityNumber) {
        // If liabilityNumber is not blank, use it; otherwise, use customer number
        String combinedString;
        if (StringUtils.isBlank(liabilityNumber)) {
            combinedString = String.format("Customer number: %s;", StringUtils.isBlank(customerNumber) ? "" : customerNumber);
        } else {
            combinedString = String.format("Document: %s;", StringUtils.isBlank(liabilityNumber) ? "" : liabilityNumber);
        }
        // Insert newlines every 110 characters
        return EPBStringUtils.insertLineBreaks(combinedString, 110);
    }

    /**
     * Formats an individual long description for a single invoice.
     * Format: "Клиентски номер: {CUSTOMER NUMBER}; Клиент: {CUSTOMER NAME FORMATTED}; Фактура: {DOC}/{DATE};"
     * Maximum length: 4000 characters (truncated if longer).
     *
     * @param customerNumber      the customer number
     * @param customerPaymentInfo customer payment info (name, legal form, UIC)
     * @param liability           single liability with invoice number and date
     * @return the formatted long description (max 4000 chars)
     */
    public static String formatIndividualLongDesc(
            String customerNumber,
            CustomerPaymentInfo customerPaymentInfo,
            CheckLiabilityDetails liability
    ) {
        String customerNameFormatted = formatCustomerNameForPayment(customerPaymentInfo);

        // Format single invoice: "DOC/DATE"
        String invoicePart = formatSingleInvoice(liability);

        String combinedString = String.format(
                "Клиентски номер: %s; Клиент: %s; Фактура: %s;",
                StringUtils.isBlank(customerNumber) ? "" : customerNumber,
                customerNameFormatted,
                invoicePart
        );

        // Truncate to 4000 characters if longer
        if (combinedString.length() > 4000) {
            return combinedString.substring(0, 4000);
        }
        return combinedString;
    }

    /**
     * Formats a combined long description for multiple invoices.
     * Format: "Клиентски номер: {CUSTOMER NUMBER}; Клиент: {CUSTOMER NAME FORMATTED}; Фактури: {DOC1}/{DATE1}, {DOC2}/{DATE2}, {DOC3}/{DATE3};"
     * Maximum length: 4000 characters (truncated if longer).
     *
     * @param customerNumber      the customer number
     * @param customerPaymentInfo customer payment info (name, legal form, UIC)
     * @param liabilities         list of liabilities with invoice numbers and dates
     * @return the formatted long description (max 4000 chars)
     */
    public static String formatCombinedLongDesc(
            String customerNumber,
            CustomerPaymentInfo customerPaymentInfo,
            List<CheckLiabilityDetails> liabilities
    ) {
        String customerNameFormatted = formatCustomerNameForPayment(customerPaymentInfo);

        String invoicesPart = formatInvoicesList(liabilities, true);
        log.info("invoicesPart - {}", invoicesPart);

        String combinedString = String.format(
                "Клиентски номер: %s; Клиент: %s; Фактури: %s;",
                StringUtils.isBlank(customerNumber) ? "" : customerNumber,
                customerNameFormatted,
                invoicesPart
        );
        log.info("combinedString - {}", combinedString);

        // Truncate to 4000 characters if longer
        if (combinedString.length() > 4000) {
            return combinedString.substring(0, 4000);
        }
        return combinedString;
    }

    /**
     * Formats a long description for invoice response (individual invoice).
     * Format: "Клиентски номер: {CUSTOMER NUMBER}; Клиент: {CUSTOMER NAME FORMATTED}; Фактура: {DOC}/{DATE};"
     * Maximum length: 4000 characters (truncated if longer).
     *
     * @param customerNumber      the customer number
     * @param customerPaymentInfo customer payment info (name, legal form, UIC)
     * @param liability           single liability with invoice number and date
     * @return the formatted long description (max 4000 chars)
     */
    public static String formatInvoiceLongDesc(
            String customerNumber,
            CustomerPaymentInfo customerPaymentInfo,
            CheckLiabilityDetails liability
    ) {
        return formatIndividualLongDesc(customerNumber, customerPaymentInfo, liability);
    }

    /**
     * Formats an invoice identifier by concatenating the provided identifier and ID with a dot.
     * This method combines the given `identifier` (typically a string, such as a customer or invoice identifier)
     * and `id` (typically a numeric ID) into a single string, separated by a dot. This is useful for creating a
     * formatted invoice identifier that includes both a string and a numeric value.
     *
     * @param identifier The string identifier, such as a customer or invoice ID.
     * @param id         The numeric identifier (e.g., an invoice or item ID) to be concatenated with the string identifier.
     * @return A formatted string combining the identifier and ID, separated by a dot.
     */
    public static String formatInvoiceIdentifier(String identifier, Long id) {
        return String.format("%s.%s", identifier, id); // Concatenates with a dot using format
    }

    /**
     * Formats customer name for payment descriptions.
     * For private customers: returns combined name.
     * For legal entities: returns "NAME LEGAL_FORM (UIC)".
     *
     * @param customerPaymentInfo customer payment info
     * @return formatted customer name
     */
    private static String formatCustomerNameForPayment(CustomerPaymentInfo customerPaymentInfo) {
        if (customerPaymentInfo == null) {
            return "";
        }

        if ("PRIVATE_CUSTOMER".equals(customerPaymentInfo.getCustomerType())) {
            // For private customers: use combined name
            return StringUtils.isBlank(customerPaymentInfo.getCustomerName()) ? "" : customerPaymentInfo.getCustomerName();
        } else {
            // For legal entities: "NAME LEGAL_FORM (UIC)"
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotBlank(customerPaymentInfo.getCustomerName())) {
                sb.append(customerPaymentInfo.getCustomerName());
            }
            if (StringUtils.isNotBlank(customerPaymentInfo.getLegalFormAbbreviation())) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(customerPaymentInfo.getLegalFormAbbreviation());
            }
            if (StringUtils.isNotBlank(customerPaymentInfo.getUic())) {
                sb.append(" (").append(customerPaymentInfo.getUic()).append(")");
            }
            return sb.toString();
        }
    }

    /**
     * Formats a list of invoices for combined payment description.
     * Format: "DOC1/DATE1, DOC2/DATE2, DOC3/DATE3"
     * Takes up to 3 invoices.
     *
     * @param liabilities list of liabilities
     * @param isCombined  true if combined payment
     * @return formatted invoices string
     */
    private static String formatInvoicesList(List<CheckLiabilityDetails> liabilities, boolean isCombined) {
        if (liabilities == null || liabilities.isEmpty()) {
            return "";
        }

        // Create a mutable copy to avoid UnsupportedOperationException when sorting immutable lists
        List<CheckLiabilityDetails> mutableList = new ArrayList<>(liabilities);
        mutableList.sort(Comparator.comparing(CheckLiabilityDetails::getLiabilityId));

        return mutableList.stream()
                .map(EPBStringUtils::formatSingleInvoice)
                .filter(StringUtils::isNotBlank)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /**
     * Formats a single invoice: "INVOICE_NUMBER/DATE"
     * Uses invoice number from invoice table (not liability number).
     *
     * @param liability liability with invoice info
     * @return formatted invoice string
     */
    private static String formatSingleInvoice(CheckLiabilityDetails liability) {
        if (liability == null) {
            return "";
        }

        // Use invoice number from invoice table (connected to customer), not liability number
        String invoiceNumber = StringUtils.isBlank(liability.getDocumentNumber())
                ? ""
                : liability.getDocumentNumber();

        String dateStr = "";
        if (liability.getDocumentDate() != null) {
            dateStr = liability.getDocumentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }

        if (!invoiceNumber.isBlank() && invoiceNumber.contains("-")) {
            invoiceNumber = invoiceNumber.split("-")[1];
        }

        if (StringUtils.isBlank(invoiceNumber) && StringUtils.isBlank(dateStr)) {
            return "";
        }

        if (dateStr.isBlank()) {
            return String.format("%s", invoiceNumber);
        }

        return String.format("%s/%s", invoiceNumber, dateStr);
    }

}
