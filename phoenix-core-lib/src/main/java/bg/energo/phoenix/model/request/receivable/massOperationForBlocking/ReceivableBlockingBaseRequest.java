package bg.energo.phoenix.model.request.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingConditionType;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReceivableBlockingBaseRequest {
    @NotBlank(message = "name-[name] should not be null or blank;")
    @Size(min = 1, max = 512, message = "name-[name] size should be between {min} and {max} characters;")
    private String name;

    @NotNull(message = "receivableBlockingTypes-[receivableBlockingTypes] should not be null;")
    private List<ReceivableBlockingType> receivableBlockingTypes;

    @NotNull(message = "receivableBlockingConditionType-[receivableBlockingCConditionType] should not be null;")
    private ReceivableBlockingConditionType receivableBlockingConditionType;

    @Size(min = 1, max = 8192, message = "listOfCustomers-[listOfCustomers] size should be between {min} and {max} characters;")
    private String listOfCustomers;

    private String conditions;

    @DuplicatedValuesValidator(fieldPath = "prefixNomenclatureIds")
    private List<Long> prefixNomenclatureIds;

    private @Valid ExclusionByAmountRequest exclusionByAmount;

    private Boolean isBlockForPayment;
    private @Valid BlockingForPaymentRequest blockingForPayment;

    private Boolean isBlockForReminderLetters;
    private @Valid BlockingForReminderLettersRequest blockingForReminderLetters;

    private Boolean isBlockForCalculation;
    private @Valid BlockingForCalculationRequest blockingForCalculation;

    private Boolean isBlockForLiabilitiesOffsetting;
    private @Valid BlockingForLiabilitiesOffsettingRequest blockingForLiabilitiesOffsetting;

    private Boolean isBlockForSupplyTermination;
    private @Valid BlockingForSupplyTerminationRequest blockingForSupplyTermination;

    @NotNull(message = "requestReceivableBlockingStatus-[requestReceivableBlockingStatus] should not be null;")
    private ReceivableBlockingStatus requestReceivableBlockingStatus;
}
