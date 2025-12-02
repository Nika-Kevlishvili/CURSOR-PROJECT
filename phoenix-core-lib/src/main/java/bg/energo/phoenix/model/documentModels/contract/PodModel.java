package bg.energo.phoenix.model.documentModels.contract;

import bg.energo.phoenix.model.documentModels.contract.response.PodResponse;

import java.math.BigDecimal;

public class PodModel {
    public String PODID;
    public String PODAdditionalID;
    public String PODName;
    public String PODAddressComb;
    public String PODAddressCombTrsl;
    public String PODPlace;
    public String PODPlaceTrsl;
    public String PODZIP;
    public String PODType;
    public String PODGO;
    public String PODConsumptionPurpose;
    public String PODMeasurementType;
    public BigDecimal EstimatedConsumption;
    public String GridOperator;

    public PodModel from(PodResponse response) {
        this.PODID = response.getPODID();
        this.PODAdditionalID = response.getPODAdditionalID();
        this.PODName = response.getPODName();
        this.PODAddressComb = response.getPODAddressComb();
        this.PODAddressCombTrsl = response.getPODAddressCombTrsl();
        this.PODPlace = response.getPODPlace();
        this.PODZIP = response.getPODZIP();
        this.PODType = response.getPODType();
        this.PODGO = response.getPODGO();
        this.PODConsumptionPurpose = response.getPODConsumptionPurpose();
        this.PODMeasurementType = response.getPODMeasurementType();
        this.EstimatedConsumption = response.getEstimatedConsumption();
        this.GridOperator=response.getPODGO();
        return this;
    }
}
