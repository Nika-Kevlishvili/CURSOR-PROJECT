package bg.energo.phoenix.model.customAnotations.product.applicationModel;

import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.MinuteRange;
import bg.energo.phoenix.model.request.product.price.aplicationModel.SettlementPeriodRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {SettlementPeriodRequestValidator.SettlementPeriodRequestValidatorImpl.class})

public @interface SettlementPeriodRequestValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class SettlementPeriodRequestValidatorImpl implements ConstraintValidator<SettlementPeriodRequestValidator, List<SettlementPeriodRequest>> {

        @Override
        public boolean isValid(List<SettlementPeriodRequest> request, ConstraintValidatorContext context) {
            if(CollectionUtils.isEmpty(request)){
                return false;
            }
            Set<MinuteRange> collect = request.stream().map(SettlementPeriodRequest::getMinuteRange).collect(Collectors.toSet());
            if(request.size()==4&&collect.size()!=request.size()){
                context.buildConstraintViolationWithTemplate("applicationModelRequest.settlementPeriodsRequest.settlementPeriods-size of list must be 4 and should contain all enum elements;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }

}
