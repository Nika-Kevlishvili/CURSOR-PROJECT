package bg.energo.phoenix.model.request.receivable.rescheduling;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.receivable.rescheduling.ReschedulingCreateRequestValidator;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingInterestType;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingStatus;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingInstallment;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingLpfs;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ReschedulingCreateRequestValidator
public class ReschedulingRequest {

    @Digits(integer = 10, fraction = 0, message = "numberOfInstallment must be a whole number;")
    @Min(value = 1, message = "numberOfInstallment must be a positive number;")
    private BigDecimal numberOfInstallment;

    @Min(value = 1, message = "amountOfTheInstallment must be a positive number;")
    private BigDecimal amountOfTheInstallment;

    @NotNull(message = "currencyId is mandatory;")
    private Long currencyId;

    private Long replaceInterestRateForLiabilitiesId;

    @NotNull(message = "interestRateForInstalmentsId is mandatory;")
    private Long interestRateForInstalmentsId;

    @NotNull(message = "installmentDueDayOfTheMonth is mandatory;")
    @Digits(integer = 2, fraction = 0, message = "installmentDueDayOfTheMonth must be a whole number;")
    @Min(value = 1, message = "installmentDueDayOfTheMonth must be between 1 and 31;")
    @Max(value = 31, message = "installmentDueDayOfTheMonth must be between 1 and 31;")
    private BigDecimal installmentDueDayOfTheMonth;

    @NotNull(message = "reschedulingInterestType is mandatory")
    private ReschedulingInterestType reschedulingInterestType;

    @NotNull(message = "customerId is mandatory;")
    private Long customerId;

    @NotNull(message = "customerDetailId is mandatory;")
    private Long customerDetailId;

    @NotNull(message = "customerCommunicationDataId-Customer communication data id must not be null;")
    private Long customerCommunicationDataId;

    @NotNull(message = "customerCommunicationDataIdForContract-Customer communication data id for contract must not be null;")
    private Long customerCommunicationDataIdForContract;

    @NotNull(message = "CustomerAssessmentId is mandatory;")
    private Long CustomerAssessmentId;

    @NotEmpty(message = "templateRequests-template requests can not be empty or null!;")
    private List<@Valid ReschedulingTemplateRequest> templateRequests;

    @DuplicatedValuesValidator(fieldPath = "files")
    private List<Long> files;

    @DuplicatedValuesValidator(fieldPath = "liabilityIdsForRescheduling")
    private List<Long> liabilityIdsForRescheduling;

    private List<ReschedulingLpfs> reschedulingLpfs;

    @NotEmpty(message = "installments-installments can not be empty or null!;")
    private List<ReschedulingInstallment> installments;

    @NotNull(message = "reschedulingStatus-reschedulingStatus is mandatory;")
    private ReschedulingStatus reschedulingStatus;

}
