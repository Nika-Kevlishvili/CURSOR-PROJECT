package bg.energo.phoenix.model.response.contract.pods;

import bg.energo.phoenix.model.enums.pod.pod.PODConsumptionPurposes;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.pod.pod.PODType;
import bg.energo.phoenix.model.response.nomenclature.pod.PodViewMeasurementType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ContractPodsResponse {
    String getIdentifier();

    String getPodDetailName();

    Long getPodDetailId();

    Long getVersionId();

    Long getPodId();

    PODType getPodType();

    String getGridOperatorName();

    PODConsumptionPurposes getPodConsumptionPurpose();

    PODMeasurementType getPodMeasurementType();

    PodViewMeasurementType getPodViewMeasurementType();

    String getBillingGroup();

    String getContractNumber();

    LocalDateTime getPodVersionCreateDate();

    LocalDate getActivationDate();

    LocalDate getDeactivationDate();

    String getDeactivationReason();

    Long getContractPodId();

    Long getRowNumber();

    Long getBillingGroupId();

    Integer getEstimatedMonthlyAvgConsumption();

    Long getDeactivationPurposeId();

    String getDealNumber();
}
