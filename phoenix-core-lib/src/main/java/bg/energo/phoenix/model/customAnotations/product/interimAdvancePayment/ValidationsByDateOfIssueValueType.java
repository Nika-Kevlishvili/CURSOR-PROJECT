package bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DateOfIssueType;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.InterimAdvancePaymentBaseRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ValidationsByDateOfIssueValueType.ValidationsByDateOfIssueValueTypeImpl.class})
public @interface ValidationsByDateOfIssueValueType {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ValidationsByDateOfIssueValueTypeImpl implements ConstraintValidator<ValidationsByDateOfIssueValueType, InterimAdvancePaymentBaseRequest> {

        /**
         * Validate that Value is in value From - Value To range and value from is not bigger than value to
         * Also validate that if Date Of Issue Type {@link DateOfIssueType} is not "DATE_OF_THE_MONTH"
         * "dateOfIssueValue", "dateOfIssueValueFrom", "dateOfIssueValueTo" must not be provided
         *
         * @param request object to validate
         * @param context context in which the constraint is evaluated
         * @return true if constraints is satisfied, else false
         */
        @Override
        public boolean isValid(InterimAdvancePaymentBaseRequest request, ConstraintValidatorContext context) {
            boolean result = true;
            if (request.getDateOfIssueType() != null) {

                context.disableDefaultConstraintViolation();

                DateOfIssueType dateOfIssueType = request.getDateOfIssueType();
                if (dateOfIssueType.equals(DateOfIssueType.DATE_OF_THE_MONTH) || dateOfIssueType.equals(DateOfIssueType.WORKING_DAYS_AFTER_INVOICE_DATE)) {

                    int maxRange = dateOfIssueType.equals(DateOfIssueType.DATE_OF_THE_MONTH)? 31 : 9999;

                    if(request.getDateOfIssueValueFrom() != null && (request.getDateOfIssueValueFrom() < 1 || request.getDateOfIssueValueFrom() > maxRange)) {
                        context.buildConstraintViolationWithTemplate("dateOfIssueValueFrom-Date of Issue Value From must be between 1_%s;".formatted(maxRange))
                                .addConstraintViolation();
                        result = false;
                    }

                    if(request.getDateOfIssueValueTo() != null && (request.getDateOfIssueValueTo() < 1 || request.getDateOfIssueValueTo() > maxRange)) {
                        context.buildConstraintViolationWithTemplate("dateOfIssueValueTo-Date of Issue Value To must be between 1_%s;".formatted(maxRange))
                                .addConstraintViolation();
                        result = false;
                    }

                    if (request.getDateOfIssueValue() != null) {
                        if(request.getDateOfIssueValue() < 1 || request.getDateOfIssueValue() > maxRange) {
                            context.buildConstraintViolationWithTemplate("dateOfIssueValue-Date of Issue Value must be between 1_%s;".formatted(maxRange))
                                    .addConstraintViolation();
                            result = false;
                        }
                        if (request.getDateOfIssueValueFrom() != null && request.getDateOfIssueValue() < request.getDateOfIssueValueFrom()) {
                            context.buildConstraintViolationWithTemplate("dateOfIssueValue-Value must not be less than value from;")
                                    .addConstraintViolation();
                            result = false;
                        }
                        if (request.getDateOfIssueValueTo() != null && request.getDateOfIssueValue() > request.getDateOfIssueValueTo()) {
                            context.buildConstraintViolationWithTemplate("dateOfIssueValue-Value must not be greater than value to;")
                                    .addConstraintViolation();
                            result = false;
                        }
                    }

                    if (request.getDateOfIssueValueFrom() != null && request.getDateOfIssueValueTo() != null && request.getDateOfIssueValueFrom() >= request.getDateOfIssueValueTo()) {
                        context.buildConstraintViolationWithTemplate("dateOfIssueValueFrom-Value from must be less than value to;")
                                .addConstraintViolation();
                        result = false;
                    }

                } else {
                    if (request.getDateOfIssueValue() != null) {
                        context.buildConstraintViolationWithTemplate("dateOfIssueValue-Date of Issue Value must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }
                    if (request.getDateOfIssueValueFrom() != null) {
                        context.buildConstraintViolationWithTemplate("dateOfIssueValueFrom-Date of Issue Value From must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }
                    if (request.getDateOfIssueValueTo() != null) {
                        context.buildConstraintViolationWithTemplate("dateOfIssueValueTo-Date of Issue Value To must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }
                }

            }
            return result;
        }
    }

}
