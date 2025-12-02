package bg.energo.phoenix.util.customer;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomerIdentifierValidator {

    private static final Integer LEGAL_FOREIGN_MAX_LENGTH = 256;

    public static boolean isValidCustomerIdentifier(String customerIdentifier,
                                                    CustomerType customerType,
                                                    Boolean foreign,
                                                    StringBuilder validationMessage) {
        if (customerType == null || foreign == null || StringUtils.isEmpty(customerIdentifier)) {
            return false;
        }

        if (customerType.equals(CustomerType.LEGAL_ENTITY)) {
            validateUIC(customerIdentifier, foreign, validationMessage);
        } else {
            validatePersonalNumber(customerIdentifier, foreign, validationMessage);
        }

        return validationMessage.isEmpty();
    }

    public static void validateUIC(String customerIdentifier, boolean foreign, StringBuilder validationMessage) {
        ArrayList<Integer> digits;
        if (!foreign) {
            try {
                digits = IdentificationNumberChecker.getNumberArrayFromString(customerIdentifier);
            } catch (Exception e) {
                validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
                return;
            }
            if (digits.size() == 9) {
                if (!IdentificationNumberChecker.checkNineDigitUICNumber(digits)) {
                    validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
                }
            } else if (digits.size() == 13) {
                if (!IdentificationNumberChecker.checkThirteenDigitUICNumber(digits)) {
                    validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
                }
            } else {
                validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
            }
        } else {
            Pattern pattern = java.util.regex.Pattern.compile("^[0-9A-Z/–-]+$");
            Matcher matcher = pattern.matcher(customerIdentifier);
            if (matcher.matches() && customerIdentifier.length() > 32) {
                validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
            }
        }
    }

    public static void validatePersonalNumber(String customerIdentifier, Boolean foreign, StringBuilder validationMessage) {
        if (foreign) {
            // needs to be parsed in "STRICT" style, otherwise invalid dates will be automatically adjusted (i.e. Apr 31 -> May 1)
            // "uuuu" in pattern means "year" instead of "year-of-era" ("yyyy")
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
            LocalDate date;
            try {
                date = LocalDate.parse(customerIdentifier.substring(0, 8), formatter);
            } catch (Exception e) {
                validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
                return;
            }

            int dayOfMonth = date.getDayOfMonth();
            Month month = date.getMonth();

            // validate month dates max range
            if (dayOfMonth > month.maxLength()) {
                validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
                return;
            }

            // validate leap year date
            if (month.equals(Month.FEBRUARY) && dayOfMonth == 29 && !Year.isLeap(date.getYear())) {
                validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
            }

            // birth year should not be before 1900
            if (date.getYear() < 1900) {
                validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
            }

            // last 4 symbols should be either digits, uppercase characters or their combination
            if (!customerIdentifier.substring(8).matches("[\\dA-Z]{4}")) {
                validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
            }
        } else {
            ArrayList<Integer> digits;
            try {
                digits = IdentificationNumberChecker.getNumberArrayFromString(customerIdentifier);
            } catch (Exception e) {
                validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
                return;
            }

            if (digits.size() != 10 || !IdentificationNumberChecker.personalNumber(digits)) {
                validationMessage.append("customerIdentifier-Customer Identifier invalid format or symbols;");
            }
        }
    }

}
