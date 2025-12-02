package bg.energo.phoenix.model.customAnotations.product.scales;

import bg.energo.phoenix.model.request.nomenclature.product.priceComponent.ScalesRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {TariffOrScaleValidator.TariffOrScaleValidatorImpl.class})
@Documented
public @interface TariffOrScaleValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class TariffOrScaleValidatorImpl implements ConstraintValidator<TariffOrScaleValidator, ScalesRequest> {
        @Override
        public boolean isValid(ScalesRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;

            String scaleCode = request.getScaleCode();
            String tariffOrScale = request.getTariffOrScale();

            if (StringUtils.isEmpty(scaleCode) && StringUtils.isBlank(tariffOrScale)) {
                context.buildConstraintViolationWithTemplate("scaleCode-[Scale Code] or [Tariff Or Scale] must be defined;").addConstraintViolation();
                context.buildConstraintViolationWithTemplate("tariffOrScale-[Scale Code] or [Tariff Or Scale] must be defined;").addConstraintViolation();
                isValid = false;
            }

            if (request.getCalculationForNumberOfDays() != null && request.getCalculationForNumberOfDays()) {
                if (request.getScaleForActiveElectricity() != null && request.getScaleForActiveElectricity()) {
                    context.buildConstraintViolationWithTemplate("scaleForActiveElectricity-[ScaleForActiveElectricity] can't be true because calculationForNumberOfDays is true;").addConstraintViolation();
                    isValid = false;
                }
            }

            if (request.getScaleForActiveElectricity() != null && request.getScaleForActiveElectricity()) {
                if (request.getCalculationForNumberOfDays() != null && request.getCalculationForNumberOfDays()) {
                    context.buildConstraintViolationWithTemplate("calculationForNumberOfDays-[CalculationForNumberOfDays] can't be true because scaleForActiveElectricity is true;").addConstraintViolation();
                    isValid = false;
                }
            }

            return isValid;
        }
    }
}
