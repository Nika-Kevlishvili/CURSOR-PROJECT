package bg.energo.phoenix.model.customAnotations.nomenclature;

import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivityJsonField;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DuplicatedSubActivityValuesValidator.DuplicatedSubActivityValuesValidatorImpl.class)
public @interface DuplicatedSubActivityValuesValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DuplicatedSubActivityValuesValidatorImpl implements ConstraintValidator<DuplicatedSubActivityValuesValidator, SubActivityJsonField> {

        @Override
        public boolean isValid(SubActivityJsonField request, ConstraintValidatorContext context) {
            String value = request.getValue();
            List<String> list = Arrays.stream(value.split("\\|")).map(x -> x.trim()).toList();
            Set<String> values=new HashSet<>();
            for (String s : list) {
                if(values.contains(s) && !s.isEmpty()){
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("value-Value contains duplicate elements!;").addConstraintViolation();
                    return false;
                }
                values.add(s);
            }
            return true;
        }
    }
}
