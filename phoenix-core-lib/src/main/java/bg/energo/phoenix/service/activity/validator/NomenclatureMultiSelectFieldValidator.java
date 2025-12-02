package bg.energo.phoenix.service.activity.validator;

import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.service.nomenclature.NomenclatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bg.energo.phoenix.util.epb.EPBFinalFields.SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER;

@Slf4j
@RequiredArgsConstructor
public class NomenclatureMultiSelectFieldValidator implements SystemActivityFieldTypeValidator {
    private final NomenclatureService nomenclatureService;

    @Override
    public void validateOnCreate(String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        List<String> selected = Arrays.stream(selectedValue.split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER)).toList();

        for (String val : selected) {
            long id = Long.parseLong(val);

            if (!nomenclatureService.existsByIdAndStatusIn(requestField.getValue(), id, List.of(NomenclatureItemStatus.ACTIVE))) {
                log.error("fields[%s].selectedValue-Provided item [ID %s] not found in nomenclature [%s] in statuses %s;"
                                  .formatted(index, id, requestField.getValue(), List.of(NomenclatureItemStatus.ACTIVE)));
                fieldValidationMessages.add("fields[%s].selectedValue-Provided item [ID %s] not found in nomenclature [%s] in statuses %s;"
                                                    .formatted(index, id, requestField.getValue(), List.of(NomenclatureItemStatus.ACTIVE)));
            }
        }
    }


    @Override
    public void validateOnEdit(String persistedValue, String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        List<Long> selected = Arrays
                .stream(selectedValue.split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER))
                .map(Long::parseLong)
                .toList();

        List<Long> toBeSearchedInActiveStatuses = new ArrayList<>();
        List<Long> toBeSearchedInActiveAndInactiveStatuses = new ArrayList<>();

        if (StringUtils.isEmpty(persistedValue)) {
            // if the value was optional and the user left it empty previously,
            // all values should be searched in active statuses
            toBeSearchedInActiveStatuses = selected;
        } else {
            // if the value is present in the persisted record, statuses should be checked
            List<Long> persistedValues = Arrays
                    .stream(persistedValue.split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER))
                    .map(Long::parseLong)
                    .toList();

            for (Long val : selected) {
                if (!persistedValues.contains(val)) {
                    toBeSearchedInActiveStatuses.add(val);
                } else {
                    toBeSearchedInActiveAndInactiveStatuses.add(val);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(toBeSearchedInActiveStatuses)) {
            for (Long id : toBeSearchedInActiveStatuses) {
                if (!nomenclatureService.existsByIdAndStatusIn(requestField.getValue(), id, List.of(NomenclatureItemStatus.ACTIVE))) {
                    log.error("fields[%s].selectedValue-Provided item [ID %s] not found in nomenclature [%s] in statuses %s;"
                                      .formatted(index, id, requestField.getValue(), List.of(NomenclatureItemStatus.ACTIVE)));
                    fieldValidationMessages.add("fields[%s].selectedValue-Provided item [ID %s] not found in nomenclature [%s] in statuses %s;"
                                                        .formatted(index, id, requestField.getValue(), List.of(NomenclatureItemStatus.ACTIVE)));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(toBeSearchedInActiveAndInactiveStatuses)) {
            for (Long id : toBeSearchedInActiveAndInactiveStatuses) {
                if (!nomenclatureService.existsByIdAndStatusIn(requestField.getValue(), id, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))) {
                    log.error("fields[%s].selectedValue-Provided item [ID %s] not found in nomenclature [%s] in statuses %s;"
                                      .formatted(index, id, requestField.getValue(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE)));
                    fieldValidationMessages.add("fields[%s].selectedValue-Provided item [ID %s] not found in nomenclature [%s] in statuses %s;"
                                                        .formatted(index, id, requestField.getValue(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE)));
                }
            }
        }
    }

}
