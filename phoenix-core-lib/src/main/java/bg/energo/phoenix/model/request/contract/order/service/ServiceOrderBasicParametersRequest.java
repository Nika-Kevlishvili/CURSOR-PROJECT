package bg.energo.phoenix.model.request.contract.order.service;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class ServiceOrderBasicParametersRequest {

    @NotNull(message = "basicParameters.serviceDetailId-Service version is mandatory;")
    private Long serviceDetailId;

    @NotNull(message = "basicParameters.customerDetailId-Customer version is mandatory;")
    private Long customerDetailId;

    @Valid
    private ServiceOrderBankingDetails bankingDetails;

    @NotNull(message = "basicParameters.interestRateId-Interest rate is mandatory;")
    private Long interestRateId;

    private Long campaignId;

    @Min(value = 1, message = "basicParameters.prepaymentTermInCalendarDays-Value must be between 1 and 9999 characters;")
    @Max(value = 9999, message = "basicParameters.prepaymentTermInCalendarDays-Value must be between 1 and 9999 characters;")
    private Integer prepaymentTermInCalendarDays;

    @NotNull(message = "basicParameters.customerCommunicationIdForBilling-Communication for billing is mandatory;")
    private Long customerCommunicationIdForBilling;

    private Long employeeId;

    @DuplicatedValuesValidator(fieldPath = "basicParameters.assistingEmployees")
    private List<Long> assistingEmployees;

    @DuplicatedValuesValidator(fieldPath = "basicParameters.internalIntermediaries")
    private List<Long> internalIntermediaries;

    @DuplicatedValuesValidator(fieldPath = "basicParameters.externalIntermediaries")
    private List<Long> externalIntermediaries;

    private List<@Valid RelatedEntityRequest> relatedEntities;

    @NotNull(message = "basicParameters.statusModifyDate-Status modify date is mandatory;")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate statusModifyDate;

    private Long invoiceTemplateId;
    private Long emailTemplateId;
}
