package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.entity.nomenclature.customer.Bank;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankResponse {
    private Long id;
    private String name;
    private String bic;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public BankResponse(Bank bank){
        this.id = bank.getId();
        this.name = bank.getName();
        this.bic = bank.getBic();
        this.orderingId = bank.getOrderingId();
        this.defaultSelection = bank.isDefaultSelection();
        this.status = bank.getStatus();
        this.systemUserId = bank.getSystemUserId();
    }
}
