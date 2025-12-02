package bg.energo.phoenix.model.request.product.price.aplicationModel;

import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.MinuteRange;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.SettlementPeriodHours;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
@Data
public class SettlementPeriodRequest {
    @NotNull(message = "applicationModelRequest.settlementPeriodsRequest.settlementPeriods.minuteRange-Minute range can not be null!;")
    private MinuteRange minuteRange;
    @Size(min = 1,message = "applicationModelRequest.settlementPeriodsRequest.settlementPeriods- at least one hours should be selected!;")
    private List<SettlementPeriodHours> hours;
}
