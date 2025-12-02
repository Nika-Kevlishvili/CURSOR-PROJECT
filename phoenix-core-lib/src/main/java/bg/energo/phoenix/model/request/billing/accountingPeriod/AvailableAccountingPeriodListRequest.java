package bg.energo.phoenix.model.request.billing.accountingPeriod;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class AvailableAccountingPeriodListRequest {

    @NotNull(message = "page-shouldn't be null")
    private Integer page;
    @NotNull(message = "size-shouldn't be null")
    private Integer size;

    private String prompt;

   /* private List<RunStage> runStages;

    private BillingType billingType;

    private LocalDate invoiceDueDateFrom;

    private LocalDate invoiceDueDateTo;

    private BillingCriteria billingCriteria;

    private BillingApplicationLevel applicationLevel;

    private LocalDate invoiceDateFrom;

    private LocalDate invoiceDateTo;*/
}
