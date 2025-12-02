package bg.energo.phoenix.model.enums.contract.express;

import lombok.Getter;

public enum ExpressCommunicationTypes {

    PERMANENT_ADDRESS("PERMANENT ADDRESS/HEADQUARTERS AND ADDRESS OF MANAGEMENT"),
    CONTRACT_COMMUNICATION("CONTRACT COMMUNICATION DATA"),
    INVOICE_ISSUANCE("INVOICE ISSUANCE DATA");
    @Getter
    private String address;

    ExpressCommunicationTypes(String address) {
        this.address = address;
    }
}
