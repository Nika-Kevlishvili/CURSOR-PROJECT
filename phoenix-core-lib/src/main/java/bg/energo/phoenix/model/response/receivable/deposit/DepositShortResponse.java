package bg.energo.phoenix.model.response.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import lombok.Data;

@Data
public class DepositShortResponse {

    private Long id;
    private String depositNumber;
    private EntityStatus status;

    public DepositShortResponse(Deposit deposit) {
        this.id = deposit.getId();
        this.depositNumber = deposit.getDepositNumber();
        this.status = deposit.getStatus();
    }

}
