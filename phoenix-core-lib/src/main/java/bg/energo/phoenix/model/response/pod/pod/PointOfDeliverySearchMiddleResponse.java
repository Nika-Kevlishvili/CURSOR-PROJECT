package bg.energo.phoenix.model.response.pod.pod;

import bg.energo.phoenix.model.enums.pod.pod.PODConsumptionPurposes;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.pod.pod.PODType;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;

public interface PointOfDeliverySearchMiddleResponse {

    String getIdentifier();
    PODType getType();

    String getCustomer();
    String getGrid();
    Integer getProvided();
    PODConsumptionPurposes getConsumption();
    PODMeasurementType getMeasurement();
    String getDisconnected();
    Long getId();
    Long getDetail();
    PodStatus getStatus();
}
