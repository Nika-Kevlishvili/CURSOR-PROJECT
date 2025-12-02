package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class ObjectionOfCbgDocumentPodImpl {
    public String Identifier;
    public String AdditionalIdentifier;
    public String CustomerIdentifier;
    public String OverdueAmount;
    public String Currency;
    public String GOGrounds;
    public String CBGGrounds;

    public ObjectionOfCbgDocumentPodImpl(String identifier, String additionalIdentifier, String customerIdentifier, BigDecimal overdueAmount, String currency, String GOGrounds, String CBGGrounds) {
        this.Identifier = identifier;
        this.AdditionalIdentifier = additionalIdentifier;
        this.CustomerIdentifier = customerIdentifier;
        this.OverdueAmount = overdueAmount != null ? overdueAmount.toString() : null;
        this.Currency = currency;
        this.GOGrounds = GOGrounds;
        this.CBGGrounds = CBGGrounds;
    }
}
