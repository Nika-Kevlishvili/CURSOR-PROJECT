package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.UnwantedCustomerReason;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class UnwantedCustomerReasonResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public UnwantedCustomerReasonResponse(UnwantedCustomerReason unwantedCustomerReason) {
        this.id = unwantedCustomerReason.getId();
        this.name = unwantedCustomerReason.getName();
        this.orderingId = unwantedCustomerReason.getOrderingId();
        this.defaultSelection = unwantedCustomerReason.isDefaultSelection();
        this.status = unwantedCustomerReason.getStatus();
        this.systemUserId = unwantedCustomerReason.getSystemUserId();
    }
}
