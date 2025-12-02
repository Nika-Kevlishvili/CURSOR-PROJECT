package bg.energo.phoenix.model.request.product.term.terms;

import bg.energo.phoenix.model.customAnotations.product.term.*;
import bg.energo.phoenix.model.customAnotations.product.terms.ValidateStartsOfContractInitialTerms;
import bg.energo.phoenix.model.customAnotations.product.terms.WaitForOldContractTermToExpireValidator;
import bg.energo.phoenix.model.enums.product.term.terms.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ContractDeliveryActivationTypeValidator
@ContractDeliveryActivationAutoTerminationValidator
@ResigningDeadlineTypeValidator
@SupplyActivationValidator
@ContractEntryIntoForceValidator
@ValidateStartsOfContractInitialTerms
@WaitForOldContractTermToExpireValidator
public class BaseTermsRequest {

    @NotBlank(message = "name-Name Invalid Format or symbols;")
    @Length(min = 1, max = 1024, message = "name-Name length must be between 1 and 1024;")
    private String name;

    @Range(min = 1, max = 9999, message = "contractDeliveryActivationValue-Contract Delivery activation Value must be in 1_9999 range;")
    private Integer contractDeliveryActivationValue;

    private ContractDeliveryActivationType contractDeliveryActivationType;

    @NotNull(message = "contractDeliveryActivationAutoTermination-Contract Delivery Activation Auto Termination is required;")
    private Boolean contractDeliveryActivationAutoTermination;

    @Range(min = 1, max = 9999, message = "resigningDeadlineValue-Resigning Deadline Value must be in 1_9999 range;")
    private Integer resigningDeadlineValue;

    private ResigningDeadlineType resigningDeadlineType;

    @NotEmpty(message = "supplyActivations-At least one element must be provided;")
    private Set<SupplyActivation> supplyActivations;

    @Range(min = 1, max = 31, message = "supplyActivationExactDateStartDay-Supply Activation Exact Date Start Day must be in 1_31 range;")
    private Integer supplyActivationExactDateStartDay;

    @Range(min = 0, max = 9999, message = "generalNoticePeriodValue-General Notice Period Value must be in 0_9999 range;")
    private Integer generalNoticePeriodValue;

    private GeneralNoticePeriodType generalNoticePeriodType;

    @Range(min = 0, max = 9999, message = "noticeTermPeriodValue-Notice Term Period Value must be in 0_9999 range;")
    private Integer noticeTermPeriodValue;

    private NoticeTermPeriodType noticeTermPeriodType;

    @Range(min = 0, max = 9999, message = "noticeTermDisconnectionPeriodValue-Notice Term Disconnection Period Value must be in 0_9999 range;")
    private Integer noticeTermDisconnectionPeriodValue;

    private NoticeTermDisconnectionPeriodType noticeTermDisconnectionPeriodType;

    @NotEmpty(message = "contractEntryIntoForces-At least one element must be provided;")
    private Set<ContractEntryIntoForce> contractEntryIntoForces;

    @Range(min = 1, max = 31, message = "contractEntryIntoForceFromExactDayOfMonthStartDay-Contract Entry Into Force From Exact Day Of Month Start Day must be in 1_31 range;")
    private Integer contractEntryIntoForceFromExactDayOfMonthStartDay;

    @NotNull(message = "noInterestOnOverdueDebts-No Interest On Overdue Debts is required;")
    private Boolean noInterestOnOverdueDebts;

    @NotEmpty(message = "startsOfContractInitialTerms-At least one element must be provided;")
    private Set<StartOfContractInitialTerm> startsOfContractInitialTerms;

    private Set<WaitForOldContractTermToExpire> waitForOldContractTermToExpires = new HashSet<>();

    @Range(min = 1, max = 31, message = "startDayOfInitialContractTerm-Start Day must be in range 1_31;")
    private Integer startDayOfInitialContractTerm;
    @Range(min = 0, max = 31, message = "startDayOfInitialContractTerm-Start Day must be in range 0_31;")
    private Integer firstDayOfTheMonthOfInitialContractTerm;

}
