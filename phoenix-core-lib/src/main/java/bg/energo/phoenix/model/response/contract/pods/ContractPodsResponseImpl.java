package bg.energo.phoenix.model.response.contract.pods;

import bg.energo.phoenix.model.enums.pod.pod.PODConsumptionPurposes;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.pod.pod.PODType;
import bg.energo.phoenix.model.response.nomenclature.pod.PodViewMeasurementType;
import bg.energo.phoenix.model.response.shared.ShortResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ContractPodsResponseImpl implements ContractPodsResponse {

    private String identifier;
    private String podDetailName;
    private Long podDetailId;
    private Long versionId;
    private Long podId;
    private PODType podType;
    private String gridOperatorName;
    private PODConsumptionPurposes podConsumptionPurpose;
    private PODMeasurementType podMeasurementType;
    private String billingGroup;
    private String contractNumber;
    private LocalDateTime podVersionCreateDate;
    private LocalDate activationDate;
    private LocalDate deactivationDate;
    private String deactivationReason;
    private Long contractPodId;
    private Long rowNumber;
    private Long billingGroupId;
    private Long deactivationPurposeId;
    private Integer estimatedMonthlyAvgConsumption;
    private PodViewMeasurementType podViewMeasurementType;
    private String dealNumber;
    private List<ShortResponse> podAdditionalParametersShortResponse;

    public ContractPodsResponseImpl(String identifier,
                                    String podDetailName,
                                    Long podDetailId,
                                    Integer versionId,
                                    Long podId,
                                    PODType podType,
                                    String gridOperatorName,
                                    PODConsumptionPurposes podConsumptionPurpose,
                                    PODMeasurementType podMeasurementType,
                                    Integer estimatedMonthlyAvgConsumption,
                                    PodViewMeasurementType podViewMeasurementType) {
        this.podViewMeasurementType = podViewMeasurementType;
        this.identifier = identifier;
        this.podDetailName = podDetailName;
        this.podDetailId = podDetailId;
        this.versionId = Long.valueOf(versionId);
        this.podId = podId;
        this.podType = podType;
        this.gridOperatorName = gridOperatorName;
        this.podConsumptionPurpose = podConsumptionPurpose;
        this.podMeasurementType = podMeasurementType;
        this.estimatedMonthlyAvgConsumption = estimatedMonthlyAvgConsumption;
    }

    // Constructor to set values
    public ContractPodsResponseImpl(String identifier,
                                    String podDetailName,
                                    Long podDetailId,
                                    Long versionId,
                                    Long podId,
                                    PODType podType,
                                    String gridOperatorName,
                                    PODConsumptionPurposes podConsumptionPurpose,
                                    PODMeasurementType podMeasurementType,
                                    String billingGroup,
                                    String contractNumber,
                                    LocalDateTime podVersionCreateDate,
                                    LocalDate activationDate,
                                    LocalDate deactivationDate,
                                    String deactivationReason,
                                    Long contractPodId,
                                    Long rowNumber,
                                    Long billingGroupId,
                                    Integer estimatedMonthlyAvgConsumption,
                                    PodViewMeasurementType podViewMeasurementType) {
        this.podViewMeasurementType = podViewMeasurementType;
        this.identifier = identifier;
        this.podDetailName = podDetailName;
        this.podDetailId = podDetailId;
        this.versionId = versionId;
        this.podId = podId;
        this.podType = podType;
        this.gridOperatorName = gridOperatorName;
        this.podConsumptionPurpose = podConsumptionPurpose;
        this.podMeasurementType = podMeasurementType;
        this.billingGroup = billingGroup;
        this.contractNumber = contractNumber;
        this.podVersionCreateDate = podVersionCreateDate;
        this.activationDate = activationDate;
        this.deactivationDate = deactivationDate;
        this.deactivationReason = deactivationReason;
        this.contractPodId = contractPodId;
        this.rowNumber = rowNumber;
        this.billingGroupId = billingGroupId;
        this.estimatedMonthlyAvgConsumption = estimatedMonthlyAvgConsumption;
    }

    public ContractPodsResponseImpl(String identifier,
                                    String podDetailName,
                                    Long podDetailId,
                                    Integer versionId,
                                    Long podId,
                                    PODType podType,
                                    String gridOperatorName,
                                    PODConsumptionPurposes podConsumptionPurpose,
                                    PODMeasurementType podMeasurementType,
                                    String billingGroup,
                                    LocalDateTime podVersionCreateDate,
                                    LocalDate activationDate,
                                    LocalDate deactivationDate,
                                    String deactivationReason,
                                    Long contractPodId,
                                    Long billingGroupId,
                                    Integer estimatedMonthlyAvgConsumption,
                                    Long deactivationPurposeId,
                                    String dealNumber) {
        this.identifier = identifier;
        this.podDetailName = podDetailName;
        this.podDetailId = podDetailId;
        this.versionId = Long.valueOf(versionId);
        this.podId = podId;
        this.podType = podType;
        this.gridOperatorName = gridOperatorName;
        this.podConsumptionPurpose = podConsumptionPurpose;
        this.podMeasurementType = podMeasurementType;
        this.billingGroup = billingGroup;
        this.podVersionCreateDate = podVersionCreateDate;
        this.activationDate = activationDate;
        this.deactivationDate = deactivationDate;
        this.deactivationReason = deactivationReason;
        this.contractPodId = contractPodId;
        this.billingGroupId = billingGroupId;
        this.deactivationPurposeId = deactivationPurposeId;
        this.estimatedMonthlyAvgConsumption = estimatedMonthlyAvgConsumption;
        this.dealNumber = dealNumber;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getPodDetailName() {
        return podDetailName;
    }

    @Override
    public Long getPodDetailId() {
        return podDetailId;
    }

    @Override
    public Long getVersionId() {
        return versionId;
    }

    @Override
    public Long getPodId() {
        return podId;
    }

    @Override
    public PODType getPodType() {
        return podType;
    }

    @Override
    public String getGridOperatorName() {
        return gridOperatorName;
    }

    @Override
    public PODConsumptionPurposes getPodConsumptionPurpose() {
        return podConsumptionPurpose;
    }

    @Override
    public PODMeasurementType getPodMeasurementType() {
        return podMeasurementType;
    }

    @Override
    public PodViewMeasurementType getPodViewMeasurementType() {
        return podViewMeasurementType;
    }

    @Override
    public String getBillingGroup() {
        return billingGroup;
    }

    @Override
    public String getContractNumber() {
        return contractNumber;
    }

    @Override
    public LocalDateTime getPodVersionCreateDate() {
        return podVersionCreateDate;
    }

    @Override
    public LocalDate getActivationDate() {
        return activationDate;
    }

    @Override
    public LocalDate getDeactivationDate() {
        return deactivationDate;
    }

    @Override
    public String getDeactivationReason() {
        return deactivationReason;
    }

    @Override
    public Long getContractPodId() {
        return contractPodId;
    }

    @Override
    public Long getRowNumber() {
        return rowNumber;
    }

    @Override
    public Long getBillingGroupId() {
        return billingGroupId;
    }

    @Override
    public Integer getEstimatedMonthlyAvgConsumption() {
        return estimatedMonthlyAvgConsumption;
    }

    @Override
    public Long getDeactivationPurposeId() {
        return deactivationPurposeId;
    }

    @Override
    public String getDealNumber() {
        return dealNumber;
    }

    public void setPodViewMeasurementType(PodViewMeasurementType podViewMeasurementType) {
        this.podViewMeasurementType = podViewMeasurementType;
    }

    public void setPodAdditionalParametersShortResponse(List<ShortResponse> podAdditionalParametersShortResponse) {
        this.podAdditionalParametersShortResponse = podAdditionalParametersShortResponse;
    }
}

