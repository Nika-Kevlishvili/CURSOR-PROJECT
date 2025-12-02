package bg.energo.phoenix.model.response.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DepositListingResponse {

    private String depositNumber;
    private String customerNumber;
    private String contractOrderNumber;
    private LocalDate paymentDeadline;
    private BigDecimal initialAmount;
    private BigDecimal currentAmount;
    private String currencyName;
    private boolean canDelete;
    private EntityStatus status;
    private Long id;

    public DepositListingResponse(DepositListingMiddleResponse middleResponse) {
        this.depositNumber = middleResponse.getDepositNumber();
        this.customerNumber = middleResponse.getCustomerNumber();
        this.contractOrderNumber = middleResponse.getContractOrderNumber();
        this.paymentDeadline = middleResponse.getPaymentDeadline();
        this.initialAmount = middleResponse.getInitialAmount();
        this.currentAmount = middleResponse.getCurrentAmount();
        this.currencyName = middleResponse.getCurrencyName();
        this.canDelete = middleResponse.isCanDelete();
        this.status = middleResponse.getStatus();
        this.id = middleResponse.getId();
    }
}
