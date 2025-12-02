package bg.energo.phoenix.model.request.nomenclature.pod;

import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProfilesFilterRequest extends NomenclatureItemsBaseFilterRequest {
    private Boolean excludeHardcodedValues;
}
