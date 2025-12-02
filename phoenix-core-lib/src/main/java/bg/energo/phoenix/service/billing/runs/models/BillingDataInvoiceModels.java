package bg.energo.phoenix.service.billing.runs.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class BillingDataInvoiceModels extends BillingDataCalculatedPriceComponents{

    private Integer invoiceNumber;
    public BillingDataInvoiceModels(BillingDataCalculatedPriceComponents calculatedPriceComponents,Integer invoiceNumber) {
        super(calculatedPriceComponents);
        this.invoiceNumber=invoiceNumber;
    }


}
