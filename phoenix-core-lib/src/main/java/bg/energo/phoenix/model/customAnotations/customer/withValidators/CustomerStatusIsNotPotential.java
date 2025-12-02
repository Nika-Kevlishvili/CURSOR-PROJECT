package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/*
    If Customer Status is not potential following fields are mandatory: Segments; Address;  Country;
    Region; Municipality; Populated Place; District; Zip Code;
    Additionally if customer type is LEGAL_ENTITY or PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY
    following fields are mandatory too: Form of Ownership; Economic Branch; Main Subject of Activity;
 */
@Target( { TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = CustomerStatusIsNotPotentialImpl.class)
public @interface CustomerStatusIsNotPotential {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
