package bg.energo.phoenix.service.billing.runs.models;

import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.product.product.ContractType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
@Data
@AllArgsConstructor
public class BillingRunForVolumesModel {

    private Long contractId;
    private Long contractDetailId;
    private ContractType contractType;
    private ContractDetailsStatus status;
    private LocalDate terminationDate;

    private Long billingGroupId;
    private String billingGroupNumber;
    private Boolean issueSeparateInvoice;
    private Long podId;
    private Long podDetailId;
    private PODMeasurementType podMeasurementType;
    private String podIdentifier;
    private LocalDate podActivationDate;
    private LocalDate podDeactivationDate;




//    private IssuedSeparateInvoice issuedSeparateInvoice;
//    private ApplicationModelType applicationModelType;
//    private ApplicationType periodType;
//    private Boolean isCorrection;
//    private Boolean isOverride;
//    private PODMeasurementType measurementType;
//    private Boolean discount;
}
