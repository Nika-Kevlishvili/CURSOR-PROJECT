package bg.energo.phoenix.model.customAnotations.contract.order.goods;


import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermType;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderPaymentTermRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermExcludes.HOLIDAYS;
import static bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermExcludes.WEEKENDS;
import static bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermType.WORKING_DAYS;
import static java.lang.annotation.ElementType.TYPE;

@Target( { TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidGoodsOrderPaymentTermValues.GoodsOrderPaymentTermValuesValidator.class})
public @interface ValidGoodsOrderPaymentTermValues {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsOrderPaymentTermValuesValidator implements ConstraintValidator<ValidGoodsOrderPaymentTermValues, GoodsOrderPaymentTermRequest> {

        private static final Integer DATE_MIN_VALUE = 0;
        private static final Integer CERTAIN_DATE_MIN_VALUE = 1;
        private static final Integer CERTAIN_DATE_MAX_VALUE = 31;
        private static final Integer WORKING_AND_CALENDAR_DAYS_MAX_VALUE = 9999;

        @Override
        public boolean isValid(GoodsOrderPaymentTermRequest request, ConstraintValidatorContext context) {
            StringBuilder errorMessage = new StringBuilder();
            Boolean isValid = true;

            GoodsOrderPaymentTermType calendarType = request.getType();
            if (calendarType == null) {
                errorMessage.append("type-Type type must not be null;");
                isValid = false;
                context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
                return isValid;
            }

            Integer value = request.getValue();

            if (value == null) {
                errorMessage.append("value-Value must not be null;");
                isValid = false;
                context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
                return isValid;
            }

            switch (calendarType) {
                case WORKING_DAYS:
                case CALENDAR_DAYS:
                    if (value < DATE_MIN_VALUE || value > WORKING_AND_CALENDAR_DAYS_MAX_VALUE) {
                        errorMessage.append(String.format("value-Value should be between %s-%s when calendar type is %s;",
                                DATE_MIN_VALUE, WORKING_AND_CALENDAR_DAYS_MAX_VALUE, calendarType.name()));
                        isValid = false;
                    }

                    if (calendarType.equals(WORKING_DAYS) &&
                            (request.getExcludes().contains(WEEKENDS) ||
                                    request.getExcludes().contains(HOLIDAYS))) {
                        errorMessage.append("excludes-Cannot exclude Weekends or Holidays when Calendar Type is set to Working Days.");
                        isValid = false;
                    }
                    break;

                case CERTAIN_DAYS:
                    if (value < CERTAIN_DATE_MIN_VALUE || value > CERTAIN_DATE_MAX_VALUE) {
                        errorMessage.append(String.format("value-Value should be between %s-%s when calendar type is %s;",
                                CERTAIN_DATE_MIN_VALUE, CERTAIN_DATE_MAX_VALUE, calendarType.name()));
                        isValid = false;
                    }
                    break;
            }

            if ((request.getExcludes() == null || request.getExcludes().isEmpty()) && request.getDueDateChange() != null) {
                errorMessage.append("dueDateChange-DueDateChange option cannot be choose when Weekends or Holidays are not selected.;");
                isValid = false;
            }

            context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
            return isValid;
        }
    }
}
