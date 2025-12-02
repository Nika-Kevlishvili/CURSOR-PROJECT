package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.EconomicBranchNCEA;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class EconomicBranchNCEAResponse {
    private Long id;
    private String name;
    private NomenclatureItemStatus status;
    private Long orderingId;
    private Boolean defaultSelection;

    public EconomicBranchNCEAResponse(EconomicBranchNCEA entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.status=entity.getStatus();
        this.orderingId = entity.getOrderingId();
        this.defaultSelection = entity.getIsDefault();
    }
}
