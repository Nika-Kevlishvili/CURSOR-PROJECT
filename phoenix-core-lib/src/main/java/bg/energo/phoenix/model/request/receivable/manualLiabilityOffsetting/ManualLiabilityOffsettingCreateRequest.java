package bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.ReceivableTemplateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
public class ManualLiabilityOffsettingCreateRequest {

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "date-date can not be null;")
    private LocalDate date;

    @NotNull(message = "customerId-Customer id  must not be null;")
    private Long customerId;

    @NotNull(message = "customerCommunicationDataId-Customer communication data id must not be null;")
    private Long customerCommunicationDataId;

    @NotNull(message = "customerDetailId-Customer detail id must not be null;")
    private Long customerDetailId;

    @Valid
    private List<ManualLiabilityOffsettingCalculateRequestData> liabilities;

    @Valid
    private List<ManualLiabilityOffsettingCalculateRequestData> payments;

    @Valid
    private List<ManualLiabilityOffsettingCalculateRequestData> receivables;

    @Valid
    private List<ManualLiabilityOffsettingCalculateRequestData> deposits;

    @Valid
    private List<ManualLiabilityOffsettingCalculateRequestData> receivedLiabilities;

    @Valid
    private List<ManualLiabilityOffsettingCalculateRequestData> receivedPayments;

    @Valid
    private List<ManualLiabilityOffsettingCalculateRequestData> receivedReceivables;

    @Valid
    private List<ManualLiabilityOffsettingCalculateRequestData> receivedDeposits;

    @Valid
    private List<ManualLiabilityOffsettingRequestOffsetData> receivedOffsets;

    private Set<ReceivableTemplateRequest> templateIds;
}
