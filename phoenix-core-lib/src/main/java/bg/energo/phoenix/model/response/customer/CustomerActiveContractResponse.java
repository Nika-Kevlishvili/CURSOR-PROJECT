package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.enums.contract.ContractType;

import java.time.LocalDateTime;


public interface CustomerActiveContractResponse {
    Long getContractId();
    String getContractNumber();
    LocalDateTime getCreationDate();
    ContractType getContractType();
}
