package bg.energo.phoenix.model.response.activity;

import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemActivityJsonFieldResponse extends SystemActivityJsonField {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ActivityNomenclatureResponse> nomenclatureValues;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FileWithStatusesResponse> files;

    public SystemActivityJsonFieldResponse(SystemActivityJsonField field) {
        this.setTitle(field.getTitle());
        this.setFieldType(field.getFieldType());
        this.setValue(field.getValue());
        this.setMandatory(field.isMandatory());
        this.setDefaultValue(field.isDefaultValue());
        this.setRegexp(field.getRegexp());
        this.setOrdering(field.getOrdering());
        this.setMaxLength(field.getMaxLength());
        this.setSelectedValue(field.getSelectedValue());
    }

}
