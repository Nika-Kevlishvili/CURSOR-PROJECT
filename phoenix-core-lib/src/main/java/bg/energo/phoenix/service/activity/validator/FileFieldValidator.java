package bg.energo.phoenix.service.activity.validator;

import bg.energo.phoenix.model.entity.activity.SystemActivity;
import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import bg.energo.phoenix.service.activity.SystemActivityFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bg.energo.phoenix.util.epb.EPBFinalFields.SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER;

@Slf4j
@RequiredArgsConstructor
public class FileFieldValidator implements SystemActivityFieldTypeValidator {
    private final SystemActivity systemActivity;
    private final SystemActivityFileService systemActivityFileService;

    @Override
    public void validateOnCreate(String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        List<Long> fileIds = validate(selectedValue, fieldValidationMessages, index);
        systemActivityFileService.attachFilesToSystemActivity(fileIds, systemActivity.getId(), fieldValidationMessages);
        systemActivityFileService.archiveFiles(systemActivity);
    }

    @Override
    public void validateOnEdit(String persistedValue, String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        List<Long> persistedFileIds = Arrays.stream(persistedValue.split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER)).map(Long::parseLong).toList();
        List<Long> fileIds = validate(selectedValue, fieldValidationMessages, index);
        systemActivityFileService.updateFiles(fileIds, persistedFileIds, systemActivity.getId(), fieldValidationMessages);
        systemActivityFileService.archiveFiles(systemActivity);
    }

    private static List<Long> validate(String selectedValue, List<String> fieldValidationMessages, int index) {
        List<Long> fileIds = new ArrayList<>();
        try {
            fileIds = Arrays.stream(selectedValue.split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER)).map(Long::parseLong).toList();
        } catch (NumberFormatException e) {
            log.error("fields[%s].selectedValue-Provided field value is not a valid file ID;".formatted(index));
            fieldValidationMessages.add("fields[%s].selectedValue-Provided field value is not a valid file ID;".formatted(index));
        }
        return fileIds;
    }

}
