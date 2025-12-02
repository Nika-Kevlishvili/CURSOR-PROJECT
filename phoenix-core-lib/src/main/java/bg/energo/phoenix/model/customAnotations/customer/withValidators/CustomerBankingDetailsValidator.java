package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
/*
    validate banking details based on Direct debit is selected or not.
    If Direct Debit is selected Bank, BIC and IBAN must be provided.
    If Not selected must not be provided
 */
@Target( { TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = CustomerBankingDetailsValidatorImpl.class)
public @interface CustomerBankingDetailsValidator {
    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
