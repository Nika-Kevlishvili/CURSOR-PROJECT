package bg.energo.phoenix.model.response.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DepositListingMiddleResponse {
    Long getId();
    String getDepositNumber();
    String getCustomerNumber();
    String getContractOrderNumber();
    LocalDate getPaymentDeadline();
    BigDecimal getInitialAmount();
    BigDecimal getCurrentAmount();
    EntityStatus getStatus();
    String getCurrencyName();
    boolean isCanDelete();
}
