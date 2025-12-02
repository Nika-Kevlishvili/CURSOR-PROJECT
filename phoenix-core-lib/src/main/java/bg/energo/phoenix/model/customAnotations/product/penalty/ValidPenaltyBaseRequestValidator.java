package bg.energo.phoenix.model.customAnotations.product.penalty;

import bg.energo.phoenix.model.request.product.penalty.penalty.PenaltyRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class ValidPenaltyBaseRequestValidator implements ConstraintValidator<ValidPenaltyRequest, PenaltyRequest> {

    @Override
    public boolean isValid(PenaltyRequest request, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        if (Objects.nonNull(request.getMinAmount()) || Objects.nonNull(request.getMaxAmount()) || StringUtils.isNotEmpty(request.getAmountFormula())) {
            if (Objects.nonNull(request.getMinAmount()) && Objects.nonNull(request.getMaxAmount())) {
                if (request.getMaxAmount().compareTo(request.getMinAmount()) < 1) {
                    context.buildConstraintViolationWithTemplate("minAmount-minAmount should be less than maxAmount;").addConstraintViolation();
                    context.buildConstraintViolationWithTemplate("maxAmount-maxAmount should be more than minAmount;").addConstraintViolation();
                    valid = false;
                }
            }
        }
        //TODO TEMPLATE for delivery purpose - should be removed
       /* if(request.getPenaltyReceivingParties()!=null && request.getPenaltyReceivingParties().contains(PartyReceivingPenalty.CUSTOMER) && request.getTemplateId()==null){
            context.buildConstraintViolationWithTemplate("templateId-[templateId] template id should be provided!;").addConstraintViolation();
            valid = false;
        }*/

        return valid;
    }
}
