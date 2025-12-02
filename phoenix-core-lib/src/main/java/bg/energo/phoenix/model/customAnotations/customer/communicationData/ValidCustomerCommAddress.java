package bg.energo.phoenix.model.customAnotations.customer.communicationData;

import bg.energo.phoenix.model.request.customer.communicationData.CustomerCommAddressRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ValidCustomerCommAddress.CustomerCommAddressValidator.class})
@Documented
public @interface ValidCustomerCommAddress {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CustomerCommAddressValidator implements ConstraintValidator<ValidCustomerCommAddress, CustomerCommAddressRequest> {
        @Override
        public boolean isValid(CustomerCommAddressRequest request,
                               ConstraintValidatorContext context) {
            if(request.getForeign() == null){
                return false;
            }

            context.disableDefaultConstraintViolation();
            boolean correct = true;
            StringBuilder stringBuilder = new StringBuilder();
            if(request.getForeign()){
                if(request.getForeignAddressData() == null){
                    stringBuilder.append("address.foreignAddressData-Foreign address data is required;");
                    correct = false;
                }
                if(request.getLocalAddressData() != null){
                    stringBuilder.append("address.localAddressData-Local address data must not be provided;");
                    correct = false;
                }
            }else{
                if(request.getForeignAddressData() != null){
                    stringBuilder.append("address.foreignAddressData-Foreign address data  must not be provided;");
                    correct = false;
                }
                if(request.getLocalAddressData() == null){
                    stringBuilder.append("address.localAddressData-Local address data is required;");
                    correct = false;
                }
            }

            context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
            return correct;
        }
    }

}
