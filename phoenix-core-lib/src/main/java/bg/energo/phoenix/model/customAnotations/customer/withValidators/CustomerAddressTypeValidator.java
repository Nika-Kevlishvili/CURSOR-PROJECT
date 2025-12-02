package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import bg.energo.phoenix.model.request.customer.CustomerAddressRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Target( { FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = CustomerAddressTypeValidator.CustomerAddressTypeValidatorImpl.class)
public @interface CustomerAddressTypeValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CustomerAddressTypeValidatorImpl implements ConstraintValidator<CustomerAddressTypeValidator,CustomerAddressRequest> {

        @Override
        public boolean isValid(CustomerAddressRequest value, ConstraintValidatorContext context) {
            if(value==null){
                return true;
            }
            Boolean foreign = value.getForeign();
            if(foreign ==null){
                return true;
            }
            String defaultConstraintMessageTemplate = context.getDefaultConstraintMessageTemplate();
            context.disableDefaultConstraintViolation();
            if(value.getForeign().equals(Boolean.FALSE)&& value.getLocalAddressData()==null){
                context.buildConstraintViolationWithTemplate(defaultConstraintMessageTemplate + "-Local address data can not be null!;").addConstraintViolation();
                return false;
            }else if (value.getForeign().equals(Boolean.TRUE)&& value.getForeignAddressData()==null){
                context.buildConstraintViolationWithTemplate(defaultConstraintMessageTemplate + "-Foreign address data can not be null!;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
