package bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel;

import bg.energo.phoenix.model.request.product.price.aplicationModel.ValueRanges;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ValueRangesValidator.ValueRangesValidatorImpl.class})
public @interface ValueRangesValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ValueRangesValidatorImpl implements ConstraintValidator<ValueRangesValidator, List<ValueRanges>> {

        @Override
        public boolean isValid(List<ValueRanges> request, ConstraintValidatorContext context) {
            int index = 0;
            if (CollectionUtils.isEmpty(request)) {
                return true;
            }
            int size = request.size();
            String getMessage = context.getDefaultConstraintMessageTemplate();
            context.disableDefaultConstraintViolation();

            request.sort((x, y) -> {
                if (x.getValueFrom() == null) {
                    return -1;
                } else if (y.getValueFrom() == null) {
                    return 1;
                } else {
                    return NumberUtils.compare(x.getValueFrom(), y.getValueFrom());
                }
            });
            for (ValueRanges valueRanges : request) {
                Integer valueFrom = valueRanges.getValueFrom();
                Integer valueTo = valueRanges.getValueTo();
                boolean valueToIsNull = valueTo == null;
                boolean valueFromIsNull = valueFrom == null;
                if (index != 0 && (valueFromIsNull)) {
                    context.buildConstraintViolationWithTemplate(String.format("%s[%s]-valueFrom should not be null!;", getMessage, index)).addConstraintViolation();
                    return false;
                }
                if (index != size - 1 && valueToIsNull) {
                    context.buildConstraintViolationWithTemplate(String.format("%s[%s]-valueTo should not be null!;", getMessage, index)).addConstraintViolation();
                    return false;
                }
                if (!valueFromIsNull && !valueToIsNull) {
                    if (valueFrom > valueTo) {
                        context.buildConstraintViolationWithTemplate(String.format("%s[%s]-valueFrom should be less than valueTo;", getMessage, index)).addConstraintViolation();
                        return false;
                    }
                    if (valueTo > 99_999_999 || valueTo < 0) {
                        context.buildConstraintViolationWithTemplate(String.format("%s[%s]-valueTo should be between 0 and 99 999 999;", getMessage, index)).addConstraintViolation();
                        return false;
                    }
                    if (valueFrom < 0) {
                        context.buildConstraintViolationWithTemplate(String.format("%s[%s]-valueFrom should be between 0 and 99 999 999;", getMessage, index)).addConstraintViolation();
                        return false;
                    }
                }

                if (index < size - 1) {
                    ValueRanges nextRange = request.get(index + 1);

                    if (valueToIsNull || (nextRange.getValueFrom() != null && nextRange.getValueFrom() <= valueTo)) {
                        context.buildConstraintViolationWithTemplate(String.format("%s[%s]-Value is overlapping;", getMessage, index)).addConstraintViolation();
                        return false;
                    }
                }

                index++;
            }
            Set<Long> currencySet = request.stream().map(ValueRanges::getCurrency).collect(Collectors.toSet());
            if (currencySet.size() > 1) {
                context.buildConstraintViolationWithTemplate(String.format("%s.currency-currency should have only one currency;", getMessage)).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}