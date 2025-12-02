package bg.energo.phoenix.service.activity.validator;

import bg.energo.phoenix.model.entity.activity.SystemActivity;
import bg.energo.phoenix.model.enums.nomenclature.SubActivityFieldType;
import bg.energo.phoenix.service.activity.SystemActivityFileService;
import bg.energo.phoenix.service.nomenclature.NomenclatureService;

public class FieldTypeValidatorFactory {

    public static SystemActivityFieldTypeValidator getValidator(SystemActivity systemActivity,
                                                                SubActivityFieldType fieldType,
                                                                NomenclatureService nomenclatureService,
                                                                SystemActivityFileService systemActivityFileService) {
        // Return the appropriate validator based on field type
        return switch (fieldType) {
            case TEXT_AREA, TEXT_BOX -> new TextFieldValidator();
            case RADIO_BUTTON, SINGLE_SELECT_DROPDOWN -> new SingleSelectFieldValidator();
            case CHECK_BOX, MULTI_SELECT_DROPDOWN -> new MultiSelectFieldValidator();
            case NOMENCLATURE_SINGLE_SELECT_DROPDOWN -> new NomenclatureSingleSelectFieldValidator(nomenclatureService);
            case NOMENCLATURE_MULTI_SELECT_DROPDOWN -> new NomenclatureMultiSelectFieldValidator(nomenclatureService);
            case DATE -> new DateFieldValidator();
            case DATE_TIME -> new DateTimeFieldValidator();
            case PHONE -> new PhoneFieldValidator();
            case EMAIL -> new EmailFieldValidator();
            case WEB -> new WebFieldValidator();
            case FILE -> new FileFieldValidator(systemActivity, systemActivityFileService);
        };
    }

}
