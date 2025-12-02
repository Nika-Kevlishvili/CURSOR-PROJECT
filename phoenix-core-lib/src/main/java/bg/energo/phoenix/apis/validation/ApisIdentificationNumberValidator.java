package bg.energo.phoenix.apis.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ApisIdentificationNumberListValidatorImpl.class)
public @interface ApisIdentificationNumberValidator {

    String message() default "identificationNumbers-List cannot contain empty or other than 9 or 13 length fields or item count is 5000 or more";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int maxListSize();

    int minListSize();

    int actualSecondLength();

    int actualFirstLength();
}