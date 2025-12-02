package bg.energo.phoenix.model.enums.contract.products;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ProductContractListingSortFields {
    ID("id"),
    CONTRACT_NUMBER("contractNumber"),
    CUSTOMER("cd.name"),
    PRODUCT_NAME("pd.name"),
    PRODUCT_TYPE("pd.productType"),
    DATE_OF_SIGNING("signingDate"),
    STATUS("contractStatus"),
    SUB_STATUS("subStatus"),
    ACTIVATION_DATE("activationDate"),
    CONTRACT_TERM_DATE("contractTermEndDate"),
    DATE_OF_ENTRY_INTO_PERPETUITY("perpetuityDate"),
    CREATION_DATE("createDate");

    @Getter
    private final String value;
}
