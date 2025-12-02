package bg.energo.phoenix.service.activity.validator;

import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;

import java.util.List;

public interface SystemActivityFieldTypeValidator {

    /**
     * Validates the selected value of the field during the creation of the activity.
     *
     * @param selectedValue           The selected value of the field.
     * @param requestField            The field that is being validated.
     * @param fieldValidationMessages The list of validation messages that will be returned to the client.
     * @param index                   The index of the field in the list of fields.
     */
    void validateOnCreate(
            String selectedValue,
            SystemActivityJsonField requestField,
            List<String> fieldValidationMessages,
            int index
    );


    /**
     * Validates the selected value of the field during the edit of the activity.
     *
     * @param persistedValue          The persisted value of the field.
     * @param selectedValue           The selected value of the field.
     * @param requestField            The field that is being validated.
     * @param fieldValidationMessages The list of validation messages that will be returned to the client.
     * @param index                   The index of the field in the list of fields.
     */
    void validateOnEdit(
            String persistedValue,
            String selectedValue,
            SystemActivityJsonField requestField,
            List<String> fieldValidationMessages,
            int index
    );

}
