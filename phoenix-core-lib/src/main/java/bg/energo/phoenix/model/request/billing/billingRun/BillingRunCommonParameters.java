package bg.energo.phoenix.model.request.billing.billingRun;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.DateTimeRangeValidator;
import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.enums.billing.billings.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class BillingRunCommonParameters {


    private BillingRunPeriodicity periodicity;

    @DuplicatedValuesValidator(message = "processPeriodicityIds")
    private List<Long> processPeriodicityIds;

    private List<RunStage> runStages;

    @Length(max = 2048, message = "commonParameters.additionalInformation-[additionalInformation] maximum length is {max};")
    private String additionalInformation;

    @DateRangeValidator(fieldPath = "commonParameters.taxEventDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate taxEventDate;

    private Long accountingPeriodId;

    @DateRangeValidator(fieldPath = "commonParameters.invoiceDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate invoiceDate;

    private InvoiceDueDateType invoiceDueDate;

    @DateRangeValidator(fieldPath = "commonParameters.dueDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate dueDate;

    @NotNull(message = "commonParameters.sendingAnInvoice-[sendingAnInvoice] is mandatory;")
    private SendingAnInvoice sendingAnInvoice;

    private ExecutionType executionType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @DateTimeRangeValidator(fieldPath = "commonParameters.executionDateAndTime", fromDateTime = "1990-01-01 00:00", toDateTime = "2090-12-31 23:59")
    private LocalDateTime executionDateAndTime;

    private Long templateId;

    private Long emailTemplateId;

    private Set<BillingNotificationRequest> billingNotifications;
}
