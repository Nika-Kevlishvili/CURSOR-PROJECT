package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationLevel;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;
import lombok.Data;

@Data
public class ApplicationModelResponse {
    private Long applicationModelId;
    private ApplicationModelType applicationModelType;
    private ApplicationType applicationType;
    private ApplicationLevel applicationLevel;
    private PerPieceResponse perPieceResponse;
    private OverTimeOneTimeResponse overTimeOneTimeResponse;
    private VolumesByScaleResponse volumesByScaleResponse;

    private OverTimePeriodicallyResponse overTimePeriodicallyResponse;
    private VolumesBySettlementPeriodResponse volumesBySettlementPeriodResponse;
    private OverTimeWithElectricityInvoiceResponse overTimeWithElectricityInvoiceResponse;

    public ApplicationModelResponse(ApplicationModelType applicationModelType, ApplicationType applicationType, ApplicationLevel applicationLevel) {
        this.applicationModelType = applicationModelType;
        this.applicationType = applicationType;
        this.applicationLevel = applicationLevel;
    }
}
