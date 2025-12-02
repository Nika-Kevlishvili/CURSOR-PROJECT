package bg.energo.phoenix.model.request.product.price.aplicationModel;

import bg.energo.phoenix.model.customAnotations.product.applicationModel.WithElectricityInvoiceValidator;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeWithElectricityPeriodType;
import lombok.Data;

@Data
@WithElectricityInvoiceValidator
public class OverTimeWithElectricityInvoiceRequest {
    private Boolean withEveryInvoice;
    private Boolean atMostOncePer;
    private OverTimeWithElectricityPeriodType overTimeWithElectricityPeriodType;
}
