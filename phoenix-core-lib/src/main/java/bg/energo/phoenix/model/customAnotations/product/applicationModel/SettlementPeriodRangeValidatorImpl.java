package bg.energo.phoenix.model.customAnotations.product.applicationModel;

import bg.energo.phoenix.model.request.product.price.aplicationModel.SettlementPeriodRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SettlementPeriodRangeValidatorImpl implements ConstraintValidator<SettlementPeriodRangeValidator, SettlementPeriodRange> {
    @Override
    public boolean isValid(SettlementPeriodRange value, ConstraintValidatorContext context) {
        if(value.getFrom()==null||value.getTo()==null){
            return true;
        }
        if(value.getFrom()>value.getTo()){
            context.buildConstraintViolationWithTemplate("applicationModelRequest.perPieceRequest.ranges.from-From value can not be greater than to value;").addConstraintViolation();
            return false;
        }
        return true;
    }
}
