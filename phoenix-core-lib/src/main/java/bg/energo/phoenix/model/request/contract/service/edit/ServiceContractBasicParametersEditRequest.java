package bg.energo.phoenix.model.request.contract.service.edit;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.contract.service.edit.ServiceContractTerminationDateValidator;
import bg.energo.phoenix.model.enums.contract.service.ContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractContractType;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ServiceContractTerminationDateValidator
public class ServiceContractBasicParametersEditRequest {
    @NotNull(message = "basicParameters.serviceId-[serviceId] is mandatory;")
    private Long serviceId;

    @NotNull(message = "basicParameters.serviceVersionId-[serviceVersionId] is mandatory;")
    private Long serviceVersionId;

    @NotNull(message = "basicParameters.contractStatus-[contractStatus] is mandatory;")
    private ServiceContractDetailStatus contractStatus;

    @NotNull(message = "basicParameters.contractStatusModifyDate-[contractStatusModifyDate] is mandatory;")
    private LocalDate contractStatusModifyDate;

    @NotNull(message = "basicParameters.contractType-[contractType] is mandatory;")
    private ServiceContractContractType contractType;

    @NotNull(message = "basicParameters.detailsSubStatus-[detailsSubStatus] is mandatory;")
    private ServiceContractDetailsSubStatus detailsSubStatus;

    //@NotNull(message = "basicParameters.signInDate-[signInDate] is mandatory;")
    @DateRangeValidator(fieldPath = "basicParameters.signInDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate signInDate;

    @DateRangeValidator(fieldPath = "basicParameters.entryIntoForceDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate entryIntoForceDate;

    @DecimalMin(value = "0.01", message = "basicParameters.contractTermUntilAmountIsReached-[contractTermUntilAmountIsReached] should be more than 0.01;")
    @DecimalMax(value = "999999999999.99", message = "basicParameters.contractTermUntilAmountIsReached-[contractTermUntilAmountIsReached] should be less than 999999999999.99;")
    private BigDecimal contractTermUntilAmountIsReached;

    @NotNull(message = "basicParameters.contractTermUntilAmountIsReachedCheckbox-[contractTermUntilAmountIsReachedCheckbox] is mandatory;")
    private Boolean contractTermUntilAmountIsReachedCheckbox;

    private Long currencyId;

    @NotNull(message = "basicParameters.customerId-[customerId] is mandatory;")
    private Long customerId;

    @NotNull(message = "basicParameters.customerVersionId-[customerVersionId] is mandatory;")
    private Long customerVersionId;

    @NotNull(message = "basicParameters.communicationDataForBilling-[communicationDataForBilling] is mandatory;")
    private Long communicationDataForBilling;

    @NotNull(message = "basicParameters.communicationDataForContract-[communicationDataForContract] is mandatory;")
    private Long communicationDataForContract;

    @NotNull(message = "basicParameters.contractVersionStatus-[contractVersionStatus] is mandatory;")
    private ContractVersionStatus contractVersionStatus;

    @Size(min = 1, message = "basicParameters.contractVersionTypes-[contractVersionTypes] should have at least one item;")
    @NotNull(message = "basicParameters.contractVersionTypes-[contractVersionTypes] is mandatory;")
    private List<Long> contractVersionTypes;

    @DateRangeValidator(fieldPath = "basicParameters.startOfTheInitialTermOfTheContract", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate startOfTheInitialTermOfTheContract;

    private LocalDate terminationDate;

    private LocalDate perpetuityDate;

    private LocalDate activationDate;

    private LocalDate contractTermEndDate;

    @NotNull(message = "basicParameters.startDate-[startDate] is mandatory;")
    private LocalDate startDate;

    private Long customerNewDetailsId;

    //SubObjects
    private List<@Valid ProxyEditRequest> proxy;
    private LinkedHashSet<Long> files;
    private LinkedHashSet<Long> documents;

    private List<@Valid RelatedEntityRequest> relatedEntities;

}
