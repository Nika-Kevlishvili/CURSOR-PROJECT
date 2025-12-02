package bg.energo.phoenix.model.request.billing.billingRun;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.billing.billings.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class BillingRunListingRequest {
    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<ExecutionType> executionType;

    private List<BillingType> billingTypes;

    private List<RunStage> automations;

    private List<BillingCriteria> billingCriteria;

    private List<BillingApplicationLevel> applicationLevels;

    private List<BillingStatus> statuses;

    private BillingRunListColumns columns;

    private BillingRunSearchFields searchFields;

    private Sort.Direction direction;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceDueDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceDueDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceDateTo;

}
