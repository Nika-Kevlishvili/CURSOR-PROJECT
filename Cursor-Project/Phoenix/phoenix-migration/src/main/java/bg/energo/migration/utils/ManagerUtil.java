package bg.energo.migration.utils;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManagerUtil {
    public static String validateJobPosition(String position) {
        String defaultPosition = "Управител";
        String validPattern =  "^[А-Яа-яA-Za-z\\d–&\\-'‘\\s]*$";

        if(position == null || position.isBlank()){
            return defaultPosition;
        }

        if(!Pattern.matches(validPattern, position)){
            StringBuilder modifiedString = new StringBuilder(position);
            Pattern pattern = Pattern.compile(validPattern);

            for (int i = 0; i < position.length(); i++) {
                char currentChar = position.charAt(i);
                Matcher matcher = pattern.matcher(String.valueOf(currentChar));

                // If the character does not match the pattern, replace it with ' '
                if (!matcher.matches()) {
                    modifiedString.setCharAt(i, ' ');
                }
            }
            return modifiedString.toString().trim();
        }

        return position.trim();
    }

    public static String validateManagerPersonalNumber(String value) {
        if (value == null) {
            return null;
        }
        int length = value.length();
        if (length == 10 && StringUtils.isNumeric(value)) {
            return value;
        } else if (length == 12) {
            // needs to be parsed in "STRICT" style, otherwise invalid dates will be automatically adjusted (i.e. Apr 31 -> May 1)
            // "uuuu" in pattern means "year" instead of "year-of-era" ("yyyy")
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
            LocalDate date;
            try {
                date = LocalDate.parse(value.substring(0, 8), formatter);
            } catch (Exception e) {
                return null;
            }

            int dayOfMonth = date.getDayOfMonth();
            Month month = date.getMonth();

            // validate month dates max range
            if (dayOfMonth > month.maxLength()) {
                return null;
            }

            // validate leap year date
            if (month.equals(Month.FEBRUARY) && dayOfMonth == 29 && !Year.isLeap(date.getYear())) {
                return null;
            }

            // birth year should not be before 1900
            if (date.getYear() < 1900) {
                return null;
            }

            // last 4 symbols should be either digits, uppercase characters or their combination
            boolean matches = value.substring(8).matches("[\\dA-Z]{4}");
            if(!matches){
                return null;
            }
        }
        return value;
    }
}
