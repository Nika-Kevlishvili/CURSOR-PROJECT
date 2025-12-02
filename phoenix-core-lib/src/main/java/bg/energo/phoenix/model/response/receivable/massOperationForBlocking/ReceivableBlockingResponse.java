package bg.energo.phoenix.model.response.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingConditionType;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingType;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceivableBlockingResponse {

    private Long id;

    private String name;

    private List<ReceivableBlockingType> blockingTypes;

    private ReceivableBlockingStatus blockingStatus;

    private EntityStatus status;

    private ReceivableBlockingConditionType blockingConditionType;

    private String listOfCustomers;

    private String conditions;

    private List<ConditionInfoShortResponse> conditionsInfo;

    private List<PrefixesShortResponse> prefixes;

    private BigDecimal lessThan;

    private BigDecimal greaterThan;

    private CurrencyShortResponse currency;

    private Boolean isBlockingForPayment;

    private BlockingForBaseResponse blockingForPayment;

    private Boolean isBlockForReminderLetters;

    private BlockingForBaseResponse blockingForReminderLetters;

    private Boolean isBlockForCalculation;

    private BlockingForBaseResponse blockingForCalculation;

    private Boolean isBlockForLiabilitiesOffsetting;

    private BlockingForBaseResponse blockingForLiabilitiesOffsetting;

    private Boolean isBlockForSupplyTermination;

    private BlockingForBaseResponse blockingForSupplyTermination;

    private List<TaskShortResponse> taskShortResponse;

}
