package bg.energo.phoenix.service.billing.runs.models;

import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingDataPriceComponents {

    private Long contractVersionId;
    private Long priceComponentId;

    private IssuedSeparateInvoice issuedSeparateInvoice;

    private ApplicationType applicationType;
    private Long applicationModelId;
}
