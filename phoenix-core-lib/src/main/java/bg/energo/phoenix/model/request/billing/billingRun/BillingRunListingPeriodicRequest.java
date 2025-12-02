package bg.energo.phoenix.model.request.billing.billingRun;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.billing.billings.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class BillingRunListingPeriodicRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<RunStage> automations;

    private List<BillingCriteria> billingCriteria;

    private List<BillingType> billingTypes;

    private List<BillingApplicationLevel> applicationLevels;

    private List<Long> periodicity;

    private BillingRunListColumns columns;

    private BillingRunSearchFields searchFields;

    private Sort.Direction direction;

}
