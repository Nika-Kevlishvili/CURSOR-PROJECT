package bg.energo.phoenix.model.customAnotations.customer.withValidators;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/*
    Validate fields based on customer Types
    IF PRIVATE_CUSTOMER following fields must not be provided: Form of Ownership; Economic Branch;
    Economic Branch NCEA; Main Subject of Activity; Business Customer Details;
    and must be provided Private Customer Details
    IF LEGAL_ENTITY or PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY must be provided Business Customer details
 */
@Target( { TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidationsByCustomerTypeImpl.class)
public @interface ValidationsByCustomerType {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
