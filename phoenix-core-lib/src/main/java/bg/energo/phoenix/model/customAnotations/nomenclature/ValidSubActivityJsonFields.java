package bg.energo.phoenix.model.customAnotations.nomenclature;

import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivityJsonField;
import bg.energo.phoenix.model.enums.nomenclature.SubActivityRegExp;
import bg.energo.phoenix.model.request.nomenclature.contract.SubActivityRequest;
import bg.energo.phoenix.util.epb.EPBListUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static bg.energo.phoenix.model.enums.nomenclature.SubActivityFieldType.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSubActivityJsonFields.SubActivityJsonFieldsValidator.class)
public @interface ValidSubActivityJsonFields {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class SubActivityJsonFieldsValidator implements ConstraintValidator<ValidSubActivityJsonFields, SubActivityRequest> {

        @Override
        public boolean isValid(SubActivityRequest request, ConstraintValidatorContext context) {
            if (CollectionUtils.isEmpty(request.getFields())) {
                return true;
            }

            StringBuilder errorMessage = new StringBuilder();
            List<SubActivityJsonField> fields = request.getFields();

            List<Integer> orderingValues = fields
                    .stream()
                    .map(SubActivityJsonField::getOrdering)
                    .toList();

            if (EPBListUtils.notAllUnique(orderingValues)) {
                errorMessage.append("fields-Ordering should be unique among fields;");
            }

            List<SubActivityJsonField> requestFields = request.getFields();
            for (int i = 0; i < requestFields.size(); i++) {
                SubActivityJsonField field = requestFields.get(i);
                if (CollectionUtils.isNotEmpty(field.getRegexp())) {
                    if (field.getRegexp().contains(SubActivityRegExp.ALL) && field.getRegexp().size() > 1) {
                        errorMessage.append("fields[%s].regexp-Regexp should not contain other values together with [ALL];".formatted(i));
                    }
                }

                if (field.isDefaultValue()) {
                    if (StringUtils.isEmpty(field.getValue())) {
                        errorMessage.append("fields[%s].value-At least one value should be provided when [defaultValue] is true;".formatted(i));
                    }
                }

                if (field.getFieldType().equals(PHONE) && StringUtils.isNotEmpty(field.getValue())) {
                    if (!field.getValue().matches(SubActivityRegExp.getPhonePattern())) {
                        errorMessage.append("fields[%s].value-Value should match phone pattern;".formatted(i));
                    }
                }

                if (field.getFieldType().equals(EMAIL) && StringUtils.isNotEmpty(field.getValue())) {
                    if (!field.getValue().matches(SubActivityRegExp.getEmailPattern())) {
                        errorMessage.append("fields[%s].value-Value should match email pattern;".formatted(i));
                    }
                }

                if (field.getFieldType().equals(WEB) && StringUtils.isNotEmpty(field.getValue())) {
                    if (!field.getValue().matches(SubActivityRegExp.getWebPattern())) {
                        errorMessage.append("fields[%s].value-Value should match web pattern;".formatted(i));
                    }
                }

                if (field.getFieldType().equals(CHECK_BOX) || field.getFieldType().equals(RADIO_BUTTON)
                        || field.getFieldType().equals(SINGLE_SELECT_DROPDOWN) || field.getFieldType().equals(MULTI_SELECT_DROPDOWN)
                        || field.getFieldType().equals(NOMENCLATURE_SINGLE_SELECT_DROPDOWN) || field.getFieldType().equals(NOMENCLATURE_MULTI_SELECT_DROPDOWN)) {
                    if (StringUtils.isEmpty(field.getValue())) {
                        errorMessage.append("fields[%s].value-Value should not be empty when field type is [%s];".formatted(i, field.getFieldType()));
                    }
                }
            }

            if (!errorMessage.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }
    }
}
