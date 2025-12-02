package bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting;

import jakarta.validation.Valid;

import java.util.List;

public record ManualLiabilityOffsettingCalculateRequest(
        @Valid List<ManualLiabilityOffsettingCalculateRequestData> liabilities,
        @Valid List<ManualLiabilityOffsettingCalculateRequestData> payments,
        @Valid List<ManualLiabilityOffsettingCalculateRequestData> receivables,
        @Valid List<ManualLiabilityOffsettingCalculateRequestData> deposits
) {
}
