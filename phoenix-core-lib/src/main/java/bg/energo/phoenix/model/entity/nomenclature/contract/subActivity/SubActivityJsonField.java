package bg.energo.phoenix.model.entity.nomenclature.contract.subActivity;

import bg.energo.phoenix.model.customAnotations.nomenclature.DuplicatedSubActivityValuesValidator;
import bg.energo.phoenix.model.enums.nomenclature.SubActivityFieldType;
import bg.energo.phoenix.model.enums.nomenclature.SubActivityRegExp;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.util.List;

@Data
@DuplicatedSubActivityValuesValidator
public class SubActivityJsonField {

    @NotNull(message = "title-Title can not be null;")
    private String title;

    @NotNull(message = "fieldType-fieldType can not be null;")
    private SubActivityFieldType fieldType;

    // options (if "defaultValue" is true, first value is considered to be default)
    private String value = "";

    private boolean mandatory;

    // means "hasDefaultValue"
    private boolean defaultValue;

    private List<SubActivityRegExp> regexp = List.of(SubActivityRegExp.ALL);

    //@Size(min = 1, max = 5, message = "ordering-ordering length should be between {min}:{max};")
    @Range(min = 1, max = 99999,message = "ordering-ordering should be between {min}:{max};")
    private Integer ordering;

    // applies to text type fields
    private Integer maxLength;

}
