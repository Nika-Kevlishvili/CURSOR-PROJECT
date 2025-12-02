package bg.energo.phoenix.model.customAnotations.product.applicationModel;

import bg.energo.common.utils.StringUtils;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.Periodicity;
import bg.energo.phoenix.model.request.product.price.aplicationModel.VolumesBySettlementPeriodsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {PeriodicityValidatorForSettlementPeriods.PeriodicityValidatorImpl.class})

public @interface PeriodicityValidatorForSettlementPeriods {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PeriodicityValidatorImpl implements ConstraintValidator<PeriodicityValidatorForSettlementPeriods, VolumesBySettlementPeriodsRequest> {

        @Override
        public boolean isValid(VolumesBySettlementPeriodsRequest request, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            Periodicity periodType = request.getPeriodType();
            if(periodType==null){
                return false;
            }
            switch (periodType){
                case DAY_OF_MONTH -> {
                    if(CollectionUtils.isEmpty(request.getDateOfMonths())){
                        context.buildConstraintViolationWithTemplate("applicationModelRequest.settlementPeriodsRequest.dateOfMonths-dateOfMonths can not be empty or null;").addConstraintViolation();
                        return false;
                    }
                }
                case RRULE_FORMULA -> {
                    if(StringUtils.isNullOrEmpty(request.getFormula())){
                        context.buildConstraintViolationWithTemplate("applicationModelRequest.settlementPeriodsRequest.formula-formula can not be empty or null;").addConstraintViolation();
                        return false;
                    }
                }
                case DAY_OF_WEEK_AND_PERIOD_OF_YEAR -> {
                    if(request.getDayOfWeekAndPeriodOfYear()==null){
                        context.buildConstraintViolationWithTemplate("applicationModelRequest.settlementPeriodsRequest.dayOfWeekAndPeriodOfYear-dayOfWeekAndPeriodOfYear can not be empty or null;").addConstraintViolation();
                        return false;
                    }

                }
            }


            return true;
        }
    }

}

