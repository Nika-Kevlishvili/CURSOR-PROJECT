package bg.energo.phoenix.model.response.billing.accountingPeriods;

import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountPeriodFileGenerationStatus;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class AccountingPeriodsPreviewResponse {

    private Long id;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private AccountingPeriodStatus status;
    private List<AccountingPeriodsChangeHistoryResponse> historyResponse;
    private List<AccountPeriodFileResponse> files;
    private AccountPeriodFileGenerationStatus fileGenerationStatus;
}
