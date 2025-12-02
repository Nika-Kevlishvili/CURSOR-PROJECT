package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.enums.billing.billings.ContractOrderType;

import java.time.LocalDateTime;

public interface CustomerContractOrderResponse {
    Long getId();
    String getNumber();
    LocalDateTime getCreationDate();
    ContractOrderType getType();
}
