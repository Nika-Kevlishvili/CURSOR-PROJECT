package bg.energo.phoenix.model.customAnotations.nomenclature;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FirstDayOfMonth.FirstDayOfMonthValidator.class)
public @interface FirstDayOfMonth {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class FirstDayOfMonthValidator implements ConstraintValidator<FirstDayOfMonth, LocalDate> {

        @Override
        public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;  // Null values are handled by @NotNull annotation
            }

            return value.getDayOfMonth() == 1;
        }
    }
}
