package bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ManualLiabilityOffsettingSaveRequest(
        @NotNull(message = "customerId-Customer id  must not be null;")
        Long customerId,
        @NotNull(message = "customerCommunicationDataId-Customer communication data id must not be null;")
        Long customerCommunicationDataId,
        @NotNull(message = "customerDetailId-Customer detail id must not be null;")
        Long customerDetailId,
        @Valid List<ManualLiabilityOffsettingCalculateRequestData> liabilities,
        @Valid List<ManualLiabilityOffsettingCalculateRequestData> receivables,
        @Valid List<ManualLiabilityOffsettingCalculateRequestData> deposits,
        @Valid List<ManualLiabilityOffsettingCalculateRequestData> receivedLiabilities,
        @Valid List<ManualLiabilityOffsettingCalculateRequestData> receivedReceivables,
        @Valid List<ManualLiabilityOffsettingCalculateRequestData> receivedDeposits,
        @Valid List<ManualLiabilityOffsettingRequestOffsetData> receivedOffsets
) {
}
