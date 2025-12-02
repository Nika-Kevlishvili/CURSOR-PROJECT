package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class BillingRunCommonParametersResponse {

    private Long id;

    private String billingNumber;

    private BillingRunPeriodicity runPeriodicity;

    private String additionalInfo;

    private BillingType type;

    private BillingStatus status;

    private LocalDate taxEventDate;

    private LocalDate invoiceDate;

    private Long accountingPeriodId;

    private String accountingPeriodName;

    private InvoiceDueDateType invoiceDueDateType;

    private LocalDate invoiceDueDate;

    private SendingAnInvoice sendingAnInvoice;

    private ExecutionType executionType;

    private LocalDateTime executionDate;

    private List<RunStage> runStages;

    private List<BillingRunProcessPeriodicityResponse> processPeriodicity;

    private ContractTemplateShortResponse templateResponse;
    private ContractTemplateShortResponse emailTemplateResponse;

    private List<BillingNotificationResponse> notificationResponses;

}
