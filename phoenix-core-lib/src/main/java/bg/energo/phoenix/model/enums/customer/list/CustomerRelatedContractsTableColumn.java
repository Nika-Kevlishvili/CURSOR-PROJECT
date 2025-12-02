package bg.energo.phoenix.model.enums.customer.list;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CustomerRelatedContractsTableColumn {
    CONTRACT_NUMBER("pc.contractNumber"),
    CONTRACT_TYPE("pc.contractType"),
    VERSION("pc.version"),
    NAME_OF_CONTRACT("pc.contractName"),
    DATE_OF_SIGNING("pc.dateOfSigning"),
    CONTRACT_STATUS("pc.contractStatus"),
    CONTRACT_SUB_STATUS("pc.contractSubStatus"),
    ACTIVATION_DATE("pc.activationDate"),
    CONTRACT_TERM_END_DATE("pc.contractTermEndDate"),
    ENTRY_INTO_FORCE_DATE("pc.entryIntoForceDate"),
    CREATION_DATE("pc.createDateForSort");

    private final String value;
}
