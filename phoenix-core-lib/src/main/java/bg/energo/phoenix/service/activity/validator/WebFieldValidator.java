package bg.energo.phoenix.service.activity.validator;

import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import bg.energo.phoenix.model.enums.nomenclature.SubActivityRegExp;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class WebFieldValidator implements SystemActivityFieldTypeValidator {

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
        if (requestField.getMaxLength() != null && selectedValue.length() > requestField.getMaxLength()) {
            log.error("fields[%s].selectedValue-Field value exceeds the max length;".formatted(index));
            fieldValidationMessages.add("fields[%s].selectedValue-Field value exceeds the max length;".formatted(index));
        }

        if (!selectedValue.matches(SubActivityRegExp.getWebPattern())) {
            log.error("fields[%s].selectedValue-Provided field value is not a valid web address;".formatted(index));
            fieldValidationMessages.add("fields[%s].selectedValue-Provided field value is not a valid web address;".formatted(index));
        }
    }

}
