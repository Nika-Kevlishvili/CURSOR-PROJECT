package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import lombok.Data;

import java.util.List;

@Data
public class ManualLiabilityOffsettingResponse {

    private List<CustomerCommunicationDataResponse> communicationDataResponse;

    private List<LiabilitiesOffsettingChoice> customerLiabilities;

    private List<LiabilitiesOffsettingChoice> negativePayments;

    private List<LiabilitiesOffsettingChoice> receivables;

    private List<LiabilitiesOffsettingChoice> deposits;
}
