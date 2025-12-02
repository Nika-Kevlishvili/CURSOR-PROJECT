package bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel;

import bg.energo.phoenix.model.request.product.price.aplicationModel.VolumeRanges;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.*;
import java.util.List;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {VolumeRangesValidator.VolumeRangesValidatorImpl.class})
public @interface VolumeRangesValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class VolumeRangesValidatorImpl implements ConstraintValidator<VolumeRangesValidator, List<VolumeRanges>> {

        @Override
        public boolean isValid(List<VolumeRanges> request, ConstraintValidatorContext context) {
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
                } else if(y.getValueFrom()==null){
                    return 1;
                }else
                {
                    return NumberUtils.compare(x.getValueFrom(), y.getValueFrom());
                }
            });
            for (VolumeRanges volumeRanges : request) {
                Integer valueFrom = volumeRanges.getValueFrom();
                Integer valueTo = volumeRanges.getValueTo();
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
                if (index < size-1) {
                    VolumeRanges nextRange = request.get(index + 1);

                    if(valueToIsNull|| (nextRange.getValueFrom() != null && nextRange.getValueFrom() <= valueTo)){
                        context.buildConstraintViolationWithTemplate(String.format("%s[%s]-Value is overlapping;", getMessage, index)).addConstraintViolation();
                        return false;
                    }
                }
                index++;
            }
            return true;
        }
    }

}
