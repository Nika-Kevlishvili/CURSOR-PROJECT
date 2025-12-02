package bg.energo.phoenix.model.request.product.price.aplicationModel;

import bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel.ApplicationModelRequestValidator;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationLevel;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;
import jakarta.validation.Valid;
import lombok.Data;

@Data
@ApplicationModelRequestValidator
public class ApplicationModelRequest {

    private ApplicationModelType applicationModelType;
    private ApplicationType applicationType;
    private ApplicationLevel applicationLevel;
    @Valid
    private ApplicationModelPerPieceRequest perPieceRequest;
    @Valid
    private OverTimeOneTimeRequest overTimeOneTimeRequest;
    @Valid
    private VolumesByScaleRequest volumesByScaleRequest;
    @Valid
    private OverTimePeriodicallyRequest overTimePeriodicallyRequest;
    @Valid
    private VolumesBySettlementPeriodsRequest settlementPeriodsRequest;
    @Valid
    private OverTimeWithElectricityInvoiceRequest overTimeWithElectricityInvoiceRequest;
}
