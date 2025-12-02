package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.OverTimeWithElectricityInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeWithElectricityPeriodType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeWithElectricityType;
import lombok.Data;

@Data
public class OverTimeWithElectricityInvoiceResponse {
    private boolean withEveryInvoice;
    private boolean atMostOncePer;
    private OverTimeWithElectricityPeriodType overTimeWithElectricityPeriodType;

    public OverTimeWithElectricityInvoiceResponse(OverTimeWithElectricityInvoice overTimeWithElectricityInvoice) {
        this.withEveryInvoice = overTimeWithElectricityInvoice.getType() == OverTimeWithElectricityType.WITH_EVERY_INVOICE;
        this.atMostOncePer = overTimeWithElectricityInvoice.getType() == OverTimeWithElectricityType.AT_MOST_ONCE;
        this.overTimeWithElectricityPeriodType = overTimeWithElectricityInvoice.getPeriodType();
    }
}
