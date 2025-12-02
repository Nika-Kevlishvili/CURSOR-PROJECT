package bg.energo.phoenix.model.customAnotations.receivable.deposit;

import bg.energo.phoenix.model.request.receivable.deposit.DepositCreateRequest;
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
@Documented
@Constraint(validatedBy = {DepositTemplateValidator.DepositTemplateValidatorImpl.class})
public @interface DepositTemplateValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DepositTemplateValidatorImpl implements ConstraintValidator<DepositTemplateValidator, DepositCreateRequest> {
        @Override
        public boolean isValid(DepositCreateRequest request, ConstraintValidatorContext context) {
            //TODO TEMPLATE for delivery purpose - should be removed
            /*if(CollectionUtils.isEmpty(request.getTemplateIds())) {
                context.buildConstraintViolationWithTemplate("templateIds-[templateIds] template ids can not be null!;").addConstraintViolation();
                return false;
            }
            Map<ReceivableTemplateType, List<ReceivableTemplateRequest>> collect = request.getTemplateIds().stream().collect(Collectors.groupingBy(x -> x.getTemplateType()));
            if(!collect.containsKey(ReceivableTemplateType.EMAIL)){
                context.buildConstraintViolationWithTemplate("templateIds-[EMAIL] Email template should be provided;").addConstraintViolation();
                return false;
            }*/
            return true;
        }


    }
}
