package bg.energo.phoenix.model.response.priceParameter;

import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterStatus;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import lombok.Data;

import java.util.List;

@Data
public class PriceParameterPreviewResponse {
    private Long id;
    private String name;
    private String priceParameterPreviewDisplayName;
    private Long version;
    private PeriodType periodType;
    private PriceParameterStatus status;
    private TimeZone timeZone;

    private List<PriceParameterDetailsVersionInfo> priceParameterDetailsVersionInfos;

    private List<PriceParameterDetailInfoPreviewResponse> priceParameterDetailInfos;

    public PriceParameterPreviewResponse(Long id, String name, String priceParameterPreviewDisplayName, Long version, PeriodType periodType, PriceParameterStatus priceParameterStatus, TimeZone timeZone) {
        this.id = id;
        this.name = name;
        this.priceParameterPreviewDisplayName = priceParameterPreviewDisplayName;
        this.version = version;
        this.periodType = periodType;
        this.status = priceParameterStatus;
        this.timeZone = timeZone;
    }
}
