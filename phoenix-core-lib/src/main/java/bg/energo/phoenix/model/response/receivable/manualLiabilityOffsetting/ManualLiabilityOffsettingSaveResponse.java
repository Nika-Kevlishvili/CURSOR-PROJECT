package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import lombok.Data;

import java.util.List;

@Data
public class ManualLiabilityOffsettingSaveResponse {

    private List<ManualLiabilityOffsettingCalculateResponseData> liabilities;
    private List<ManualLiabilityOffsettingCalculateResponseData> receivables;
    private List<ManualLiabilityOffsettingCalculateResponseData> deposits;
    private List<ManualLiabilityOffsettingCalculateOffsetResponse> offsets;
    private String message;
    private Long id;
}
