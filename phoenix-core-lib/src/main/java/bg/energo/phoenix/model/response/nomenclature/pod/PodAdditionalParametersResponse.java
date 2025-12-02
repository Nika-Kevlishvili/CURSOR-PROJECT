package bg.energo.phoenix.model.response.nomenclature.pod;

import bg.energo.phoenix.model.entity.nomenclature.pod.PodAdditionalParameters;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PodAdditionalParametersResponse {

    private Long id;
    private String name;
    private Long orderingId;
    private Boolean defaultSelection;
    private NomenclatureItemStatus status;

    public PodAdditionalParametersResponse(PodAdditionalParameters productTypes) {
        this.id = productTypes.getId();
        this.name = productTypes.getName();
        this.orderingId = productTypes.getOrderingId();
        this.defaultSelection = productTypes.getIsDefault();
        this.status = productTypes.getStatus();
    }

}
