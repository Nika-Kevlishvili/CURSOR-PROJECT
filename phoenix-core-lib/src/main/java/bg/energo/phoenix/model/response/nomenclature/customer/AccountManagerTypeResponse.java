package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.AccountManagerType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class AccountManagerTypeResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public AccountManagerTypeResponse(AccountManagerType accountManagerType) {
        this.id = accountManagerType.getId();
        this.name = accountManagerType.getName();
        this.orderingId = accountManagerType.getOrderingId();
        this.defaultSelection = accountManagerType.isDefaultSelection();
        this.status = accountManagerType.getStatus();
        this.systemUserId = accountManagerType.getSystemUserId();
    }
}
