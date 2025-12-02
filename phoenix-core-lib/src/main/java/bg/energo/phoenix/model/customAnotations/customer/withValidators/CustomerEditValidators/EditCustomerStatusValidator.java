package bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerEditValidators;

import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.TYPE;

@Target( { TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = EditValidationsByCustomerTypeImpl.class)
public @interface EditCustomerStatusValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class EditCustomerStatusValidatorImpl implements ConstraintValidator<EditCustomerStatusValidator, EditCustomerRequest> {

        @Override
        public boolean isValid(EditCustomerRequest value, ConstraintValidatorContext context) {
            CustomerDetailStatus customerDetailStatus = value.getCustomerDetailStatus();
            Boolean updateExistingVersion = value.getUpdateExistingVersion();
            if(customerDetailStatus==null){
                return true;
            }
            if(customerDetailStatus==null){
                return true;
            }

            if(updateExistingVersion.equals(Boolean.FALSE) && !customerDetailStatus.equals(CustomerDetailStatus.POTENTIAL)){
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("customerDetailStatus-Customer status can only be POTENTIAL!;");
                return false;
            }
            if(List.of(CustomerDetailStatus.ACTIVE,CustomerDetailStatus.LOST).contains(customerDetailStatus)){
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("customerDetailStatus-Customer status can not be changed to ACTIVE or LOST!;");
                return false;
            }
            return true;
        }
    }
}
