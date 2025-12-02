package bg.energo.phoenix.service.activity.validator;

import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.service.nomenclature.NomenclatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bg.energo.phoenix.util.epb.EPBFinalFields.SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER;

@Slf4j
@RequiredArgsConstructor
public class NomenclatureSingleSelectFieldValidator implements SystemActivityFieldTypeValidator {
    private final NomenclatureService nomenclatureService;

    @Override
    public void validateOnCreate(String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        List<String> selected = Arrays.stream(selectedValue.split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER)).toList();

        if (selected.size() > 1) {
            log.error("fields[%s].selectedValue-Only single value should be selected;".formatted(index));
            fieldValidationMessages.add("fields[%s].selectedValue-Only single value should be selected;".formatted(index));
        }

        if (!nomenclatureService.existsByIdAndStatusIn(requestField.getValue(), Long.parseLong(selected.get(0)), List.of(NomenclatureItemStatus.ACTIVE))) {
            log.error("fields[%s].selectedValue-Provided item [ID %s] not found in nomenclature [%s] in statuses %s;"
                              .formatted(index, selected.get(0), requestField.getValue(), List.of(NomenclatureItemStatus.ACTIVE)));
            fieldValidationMessages.add("fields[%s].selectedValue-Provided item [ID %s] not found in nomenclature [%s] in statuses %s;"
                                                .formatted(index, selected.get(0), requestField.getValue(), List.of(NomenclatureItemStatus.ACTIVE)));
        }
    }


    @Override
    public void validateOnEdit(String persistedValue, String selectedValue, SystemActivityJsonField requestField, List<String> fieldValidationMessages, int index) {
        List<String> selected = Arrays.stream(selectedValue.split(SYSTEM_ACTIVITY_MULTIPLE_VALUES_DELIMITER)).toList();

        if (selected.size() > 1) {
            log.error("fields[%s].selectedValue-Only single value should be selected;".formatted(index));
            fieldValidationMessages.add("fields[%s].selectedValue-Only single value should be selected;".formatted(index));
        }

        List<NomenclatureItemStatus> statuses = new ArrayList<>();
        if (StringUtils.isEmpty(persistedValue) || !selectedValue.equals(persistedValue)) {
            // if the value was optional and the user left it empty previously, or if the value was changed
            statuses.add(NomenclatureItemStatus.ACTIVE);
        } else { // if the value was not changed
            statuses.add(NomenclatureItemStatus.ACTIVE);
            statuses.add(NomenclatureItemStatus.INACTIVE);
        }

        if (!nomenclatureService.existsByIdAndStatusIn(requestField.getValue(), Long.parseLong(selected.get(0)), statuses)) {
            log.error("fields[%s].selectedValue-Provided item [ID %s] not found in nomenclature [%s] in statuses %s;"
                              .formatted(index, selected.get(0), requestField.getValue(), statuses));
            fieldValidationMessages.add("fields[%s].selectedValue-Provided item [ID %s] not found in nomenclature [%s] in statuses %s;"
                                                .formatted(index, selected.get(0), requestField.getValue(), statuses));
        }
    }

}
