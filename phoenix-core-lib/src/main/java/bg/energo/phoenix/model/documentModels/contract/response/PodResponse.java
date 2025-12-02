package bg.energo.phoenix.model.documentModels.contract.response;

import java.math.BigDecimal;

public interface PodResponse {
    String getPODID();

    String getPODAdditionalID();

    String getPODName();

    String getPODAddressComb();

    String getPODAddressCombTrsl();

    String getPODPlace();

    String getPODZIP();

    String getPODType();

    String getPODGO();

    String getPODConsumptionPurpose();

    String getPODMeasurementType();

    BigDecimal getEstimatedConsumption();

    String getPodState();
}
