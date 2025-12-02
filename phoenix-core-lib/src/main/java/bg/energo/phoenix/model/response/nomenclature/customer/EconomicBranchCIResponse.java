package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.EconomicBranchCI;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class EconomicBranchCIResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public EconomicBranchCIResponse(EconomicBranchCI economicBranchCI) {
        this.id = economicBranchCI.getId();
        this.name = economicBranchCI.getName();
        this.orderingId = economicBranchCI.getOrderingId();
        this.defaultSelection = economicBranchCI.isDefaultSelection();
        this.status = economicBranchCI.getStatus();
        this.systemUserId = economicBranchCI.getSystemUserId();
    }
}
