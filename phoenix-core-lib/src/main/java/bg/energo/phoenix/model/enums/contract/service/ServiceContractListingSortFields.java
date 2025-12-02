package bg.energo.phoenix.model.enums.contract.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ServiceContractListingSortFields {
    ID("id"),
    CONTRACT_NUMBER("contractNumber"),
    CUSTOMER("cd.name"),
    SERVICE_NAME("sd.name"),
    SERVICE_TYPE("sd.serviceType"),
    DATE_OF_SIGNING("signingDate"),
    STATUS("contractStatus"),
    SUB_STATUS("subStatus"),
//    ACTIVATION_DATE("sc.activationDate"),
    CONTRACT_TERM_DATE("contractTermEndDate"),
    DATE_OF_ENTRY_INTO_PERPETUITY("perpetuityDate"),
    CREATION_DATE("createDate");

    @Getter
    private final String value;
}
