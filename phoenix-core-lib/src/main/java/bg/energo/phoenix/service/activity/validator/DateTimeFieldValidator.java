package bg.energo.phoenix.service.activity.validator;

import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import bg.energo.phoenix.util.epb.EPBDateUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class DateTimeFieldValidator implements SystemActivityFieldTypeValidator {

    @Override
    public void validateOnCreate(String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        validate(selectedValue, fieldValidationMessages, index);
    }


    @Override
    public void validateOnEdit(String persistedValue, String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        validate(selectedValue, fieldValidationMessages, index);
    }


    // validation is the same for create and edit at the moment
    private static void validate(String selectedValue, List<String> fieldValidationMessages, int index) {
        if (!EPBDateUtils.isValidLocalDateTime(selectedValue, DateTimeFormatter.ISO_DATE_TIME)) {
            log.error("fields[%s].selectedValue-Provided field value is not a valid date time;".formatted(index));
            fieldValidationMessages.add("fields[%s].selectedValue-Provided field value is not a valid date time;".formatted(index));
        }
    }

}
