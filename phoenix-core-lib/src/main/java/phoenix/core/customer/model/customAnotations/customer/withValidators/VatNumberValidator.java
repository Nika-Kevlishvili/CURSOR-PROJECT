package phoenix.core.customer.model.customAnotations.customer.withValidators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

/*
    Check VAT Number format based on customer Type
    If LEGAL_ENTITY length must be 11 or 15
    If PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY length must be 12 or 14
 */
@Target( { TYPE, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {VatNumberValidatorImpl.class})
public @interface VatNumberValidator {
    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
