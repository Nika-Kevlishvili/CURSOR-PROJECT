package bg.energo.phoenix.model.customAnotations.customer.manager;

import bg.energo.phoenix.util.epb.EPBFinalFields;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target( { FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PersonalNumber.PersonalNumberValidator.class})
public @interface PersonalNumber {
    String message() default "PersonalNumber length must be 10 or 12 digits and match valid pattern;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PersonalNumberValidator implements ConstraintValidator<PersonalNumber, String> {

        /**
         * Checks whether the input value satisfies the following conditions:
         * <ul>
         *     <li>If the length of the input value is 10, it should only contain digits.</li>
         *     <li>If the length of the input value is 12, the first 8 characters should be digits and
         *     should match the pattern YYYYMMDD where YYYY means year (should be less than 1900),
         *     MM means month, and DD means day (additional constraints are applied to check February date validity).
         *     The last 4 characters can be digits or uppercase Latin characters.</li>
         * </ul>
         * @param value input value to be validated representing a personal number
         * @param context context in which the constraint is evaluated
         *
         * @return {@code true} if the input value satisfies the above conditions, {@code false} otherwise
         */
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if(StringUtils.equals(value, EPBFinalFields.GDPR) || StringUtils.isEmpty(value)){
                return true;
            }

            int length = value.length();
            if (length == 10 && StringUtils.isNumeric(value)) {
                return true;
            } else if (length == 12) {
                // needs to be parsed in "STRICT" style, otherwise invalid dates will be automatically adjusted (i.e. Apr 31 -> May 1)
                // "uuuu" in pattern means "year" instead of "year-of-era" ("yyyy")
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
                LocalDate date;
                try {
                    date = LocalDate.parse(value.substring(0, 8), formatter);
                } catch (Exception e) {
                    return false;
                }

                int dayOfMonth = date.getDayOfMonth();
                Month month = date.getMonth();

                // validate month dates max range
                if (dayOfMonth > month.maxLength()) {
                    return false;
                }

                // validate leap year date
                if (month.equals(Month.FEBRUARY) && dayOfMonth == 29 && !Year.isLeap(date.getYear())) {
                    return false;
                }

                // birth year should not be before 1900
                if (date.getYear() < 1900) {
                    return false;
                }

                // last 4 symbols should be either digits, uppercase characters or their combination
                return value.substring(8).matches("[\\dA-Z]{4}");
            }
            return false;
        }

    }
}
