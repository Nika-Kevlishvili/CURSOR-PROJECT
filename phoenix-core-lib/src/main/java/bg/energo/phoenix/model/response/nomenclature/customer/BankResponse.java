package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
