package bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.edit;

import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.CreateInterimAdvancePaymentTermRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.EditInterimAdvancePaymentRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {EditPaymentTermValidator.EditPaymentTermValidatorImpl.class})
public @interface EditPaymentTermValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class EditPaymentTermValidatorImpl implements ConstraintValidator<EditPaymentTermValidator, EditInterimAdvancePaymentRequest> {

        /**
         * Validate That: If "matchesWithTermOfStandardInvoice" field is true request of payment term is not provided.
         * If "matchesWithTermOfStandardInvoice" field is false request of payment term must be provided {@link CreateInterimAdvancePaymentTermRequest}
         *
         * @param request object to validate
         * @param context context in which the constraint is evaluated
         *
         * @return false if constraints are violated, else true
         */
        @Override
        public boolean isValid(EditInterimAdvancePaymentRequest request, ConstraintValidatorContext context) {
            if(request.getMatchesWithTermOfStandardInvoice() != null){
                context.disableDefaultConstraintViolation();
                if(request.getMatchesWithTermOfStandardInvoice()){
                    if(request.getInterimAdvancePaymentTerm() != null){
                        context.buildConstraintViolationWithTemplate("interimAdvancePaymentTermRequest-You can't create/edit payment term while \"Matches with term of standard invoice\" is selected;")
                                .addConstraintViolation();
                        return false;
                    }
                }else{
                    if(request.getInterimAdvancePaymentTerm() == null){
                        context.buildConstraintViolationWithTemplate("interimAdvancePaymentTermRequest-Creating/Editing payment term is required while \"Matches with term of standard invoice\" is not selected;")
                                .addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }

}
