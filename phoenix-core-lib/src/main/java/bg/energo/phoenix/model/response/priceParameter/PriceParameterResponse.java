package bg.energo.phoenix.model.response.priceParameter;

import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameter;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterStatus;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceParameterResponse {
    private Long id;
    private PeriodType periodType;
    private TimeZone timeZone;
    private PriceParameterStatus status;
    private Long lastPriceParameterDetailId;

    public PriceParameterResponse(PriceParameter priceParameter) {
        this.id = priceParameter.getId();
        this.periodType = priceParameter.getPeriodType();
        this.timeZone = priceParameter.getTimeZone();
        this.status = priceParameter.getStatus();
        this.lastPriceParameterDetailId = priceParameter.getLastPriceParameterDetailId();
    }
}
