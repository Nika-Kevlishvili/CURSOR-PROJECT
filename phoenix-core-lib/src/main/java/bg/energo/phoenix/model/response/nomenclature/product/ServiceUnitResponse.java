package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceUnit;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceUnitResponse {

    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;

    public ServiceUnitResponse(ServiceUnit serviceUnit) {
        this.id = serviceUnit.getId();
        this.name = serviceUnit.getName();
        this.orderingId = serviceUnit.getOrderingId();
        this.defaultSelection = serviceUnit.isDefaultSelection();
        this.status = serviceUnit.getStatus();
    }

}
