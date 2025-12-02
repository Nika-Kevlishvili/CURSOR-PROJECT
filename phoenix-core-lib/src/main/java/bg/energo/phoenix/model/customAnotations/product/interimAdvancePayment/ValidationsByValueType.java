package bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.ValueType;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.InterimAdvancePaymentBaseRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ValidationsByValueType.ValidationsByValueTypeImpl.class})
public @interface ValidationsByValueType {


    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ValidationsByValueTypeImpl implements ConstraintValidator<ValidationsByValueType, InterimAdvancePaymentBaseRequest> {

        /**
         * Validate that required fields are provided according to chosen value type {@link ValueType}
         * and not allowed fields are not provided.
         * If Value Type {@link ValueType} is Price Component, priceComonentId is required
         * and "value", "valueFrom", "valueTo" and "currencyId" must not be provided.
         * If Value Type is not Price Component priceComponentId must not be provided and currencyId is required.
         *
         * @param request object to validate
         * @param context context in which the constraint is evaluated
         *
         * @return true if constraints are satisfies, else false
         */
        @Override
        public boolean isValid(InterimAdvancePaymentBaseRequest request, ConstraintValidatorContext context) {
            boolean result = true;
            if(request.getValueType() != null){
                context.disableDefaultConstraintViolation();
                if(request.getValueType().equals(ValueType.PRICE_COMPONENT)){

                    if(request.getPriceComponentId() == null){
                        context.buildConstraintViolationWithTemplate("priceComponentId-Price Component is required;")
                                .addConstraintViolation();
                        result = false;
                    }

                    if(request.getValue() != null){
                        context.buildConstraintViolationWithTemplate("value-Value must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }
                    if(request.getValueFrom() != null){
                        context.buildConstraintViolationWithTemplate("valueFrom-Value From must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }
                    if(request.getValueTo() != null){
                        context.buildConstraintViolationWithTemplate("valueTo-Value To must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }
                    if(request.getCurrencyId() != null){
                        context.buildConstraintViolationWithTemplate("currencyId-Currency must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }

                }else {

                    if(request.getCurrencyId() == null){
                        context.buildConstraintViolationWithTemplate("currencyId-Currency is required;")
                                .addConstraintViolation();
                        result = false;
                    }

                    if(request.getPriceComponentId() != null){
                        context.buildConstraintViolationWithTemplate("priceComponentId-Price Component must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }

                    if(request.getValue() != null){
                        if(request.getValueFrom() != null && request.getValue().compareTo(request.getValueFrom()) < 0){
                            context.buildConstraintViolationWithTemplate("value-Value must not be less than value from;")
                                    .addConstraintViolation();
                            result = false;
                        }
                        if(request.getValueTo() != null && request.getValue().compareTo(request.getValueTo()) > 0){
                            context.buildConstraintViolationWithTemplate("value-Value must not be greater than value to;")
                                    .addConstraintViolation();
                            result = false;
                        }
                    }

                    if(request.getValueFrom() != null && request.getValueTo() != null && request.getValueFrom().compareTo(request.getValueTo()) >= 0){
                        context.buildConstraintViolationWithTemplate("valueFrom-Value from must be less than value to;")
                                .addConstraintViolation();
                        result = false;
                    }

                }
            }

            return result;
        }
    }

}
