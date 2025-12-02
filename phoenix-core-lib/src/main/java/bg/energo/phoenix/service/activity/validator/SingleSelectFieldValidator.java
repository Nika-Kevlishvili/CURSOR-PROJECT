package bg.energo.phoenix.service.activity.validator;

import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;
import java.util.List;

import static bg.energo.phoenix.util.epb.EPBFinalFields.SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER;

@Slf4j
public class SingleSelectFieldValidator implements SystemActivityFieldTypeValidator {

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
        List<String> options = Arrays.stream(requestField.getValue().split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER)).toList();
        List<String> selected = Arrays.stream(selectedValue.split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER)).toList();

        if (selected.size() > 1) {
            log.error("fields[%s].selectedValue-Single value should be selected;".formatted(index));
            fieldValidationMessages.add("fields[%s].selectedValue-Single value should be selected;".formatted(index));
        }

        if (!CollectionUtils.containsAll(options, selected)) {
            log.error("fields[%s].selectedValue-Provided field value is not in the list of options;".formatted(index));
            fieldValidationMessages.add("fields[%s].selectedValue-Provided field value is not in the list of options;".formatted(index));
        }
    }

}
