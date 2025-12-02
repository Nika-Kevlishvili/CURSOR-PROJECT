package bg.energo.phoenix.model.enums.customer.list;

import lombok.Getter;

public enum CustomerRelatedLiabilityAndReceivableSearchField {

    ALL("ALL"),
    ID("ID"),
    OUTGOING_DOCUMENT("OUTGOIGDOCUMENT"),
    CONTRACT_ORDER_NUMBER("CONTRACTORDER"),
    POD_IDENTIFIER("PODIDENTIFIER"),
    POD_ADDRESS("PODADDRESS");

    @Getter
    private String value;

    CustomerRelatedLiabilityAndReceivableSearchField(String value) {
        this.value = value;
    }
}
