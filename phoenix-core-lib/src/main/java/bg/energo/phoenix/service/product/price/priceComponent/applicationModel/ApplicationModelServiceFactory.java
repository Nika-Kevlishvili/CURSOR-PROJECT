package bg.energo.phoenix.service.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationModelServiceFactory {

    private final PerPieceService perPieceService;
    private final OverTimeOneTimeService overTimeOneTimeService;
    private final OverTimePeriodicallyService overTimePeriodicallyService;
    private final VolumesByScalesService volumesByScalesService;
    private final VolumesBySettlementPeriodService volumesBySettlementPeriodService;
    private final OverTimeWithElectricityInvoiceService overTimeWithElectricityInvoiceService;

    public ApplicationModelBaseService getModelService(ApplicationModelType modelType, ApplicationType type) {

        if (modelType.equals(ApplicationModelType.PRICE_AM_PER_PIECE)) {
            return perPieceService;
        } else if (modelType.equals(ApplicationModelType.PRICE_AM_OVERTIME) && type.equals(ApplicationType.ONE_TIME)) {
            return overTimeOneTimeService;
        } else if (modelType.equals(ApplicationModelType.PRICE_AM_OVERTIME) && type.equals(ApplicationType.PERIODICALLY)) {
            return overTimePeriodicallyService;
        } else if (modelType.equals(ApplicationModelType.PRICE_AM_FOR_VOLUMES) && type.equals(ApplicationType.BY_SCALES)) {
            return volumesByScalesService;
        } else if (modelType.equals(ApplicationModelType.PRICE_AM_FOR_VOLUMES) && type.equals(ApplicationType.BY_SETTLEMENT_PERIODS)) {
            return volumesBySettlementPeriodService;
        } else if (modelType.equals(ApplicationModelType.PRICE_AM_OVERTIME) && type.equals(ApplicationType.WITH_ELECTRICITY_INVOICE)) {
            return overTimeWithElectricityInvoiceService;
        }
        throw new ClientException("Application model service not found!", ErrorCode.APPLICATION_ERROR);
    }

}
