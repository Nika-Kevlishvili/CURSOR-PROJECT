package bg.energo.phoenix.service.activity.validator;

import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import bg.energo.phoenix.model.enums.nomenclature.SubActivityRegExp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Slf4j
public class TextFieldValidator implements SystemActivityFieldTypeValidator {

    @Override
    public void validateOnCreate(String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        validate(selectedValue, requestField, fieldValidationMessages, index);
    }


    @Override
    public void validateOnEdit(String persistedValue, String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        validate(selectedValue, requestField, fieldValidationMessages, index);
    }


    // validation is the same for create and edit at the moment
    private static void validate(String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        if (CollectionUtils.isNotEmpty(requestField.getRegexp())) {
            if (!selectedValue.matches(SubActivityRegExp.getPatternByRegExp(requestField.getRegexp()))) {
                log.error("fields[%s].selectedValue-Field value does not match the regexp;".formatted(index));
                fieldValidationMessages.add("fields[%s].selectedValue-Field value does not match the regexp;".formatted(index));
            }
        }

        if (requestField.getMaxLength() != null && selectedValue.length() > requestField.getMaxLength()) {
            log.error("fields[%s].selectedValue-Field value exceeds the max length;".formatted(index));
            fieldValidationMessages.add("fields[%s].selectedValue-Field value exceeds the max length;".formatted(index));
        }
    }

}
