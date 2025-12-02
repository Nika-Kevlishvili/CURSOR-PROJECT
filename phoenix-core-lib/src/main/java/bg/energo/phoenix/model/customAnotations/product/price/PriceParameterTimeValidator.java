package bg.energo.phoenix.model.customAnotations.product.price;


import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.request.product.price.priceParameter.PriceParameterTimeRequest;
import bg.energo.phoenix.model.request.product.price.priceParameter.PriceParameterUpdateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.util.List;

public class PriceParameterTimeValidator implements ConstraintValidator<PriceTimeUpdateValidator, PriceParameterUpdateRequest> {
    @Override
    public boolean isValid(PriceParameterUpdateRequest requests, ConstraintValidatorContext context) {
        PeriodType periodType = requests.getPeriodType();
        return isValidPeriodFrom(requests.getPriceParameterDetails(),requests.getPeriodType(),context);
    }

    private boolean isValidPeriodFrom(List<PriceParameterTimeRequest> requests, PeriodType periodType,ConstraintValidatorContext context) {
        if(requests==null){
            return true;
        }
        for (PriceParameterTimeRequest timeRequest : requests) {
            LocalDateTime periodFrom = timeRequest.getPeriodFrom();
            if(periodFrom==null){
                context.buildConstraintViolationWithTemplate("").addConstraintViolation();
                return false;
            }
            int second = periodFrom.getSecond();
            int minute = periodFrom.getMinute();
            int hour = periodFrom.getHour();
            if(periodFrom.getSecond()!=0){
                context.buildConstraintViolationWithTemplate("periodFrom-invalid seconds in periodFrom; ").addConstraintViolation();
                return false;
            }
            if (periodType.equals(PeriodType.FIFTEEN_MINUTES)) {
                if (!(minute == 15 || minute == 30 || minute == 45 || minute == 0)) {
                    context.buildConstraintViolationWithTemplate("periodFrom-invalid minutes in periodFrom; ").addConstraintViolation();
                    return false;
                }
            } else if (periodType.equals(PeriodType.ONE_HOUR)) {
                if (minute != 0) {
                    context.buildConstraintViolationWithTemplate("periodFrom-invalid hours in periodFrom; ").addConstraintViolation();
                    return false;
                }
            } else if (periodType.equals(PeriodType.ONE_DAY)) {
                if (hour != 0 || minute != 0) {
                    context.buildConstraintViolationWithTemplate("periodFrom-invalid minutes or hours in periodFrom; ").addConstraintViolation();
                    return false;
                }
            } else if (periodType.equals(PeriodType.ONE_MONTH)) {
                if (periodFrom.getDayOfMonth() != 1 || hour != 0 || minute != 0) {
                    context.buildConstraintViolationWithTemplate("periodFrom-invalid minutes or hours or days in periodFrom; ").addConstraintViolation();
                    return false;
                }
            }

        }
        return true;
    }
}
