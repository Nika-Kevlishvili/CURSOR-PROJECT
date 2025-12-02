package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ManualLiabilityOffsettingPreviewResponse {

    private Long idNumber;

    private Long customerId;

    private boolean reversed;

    private List<CustomerCommunicationDataResponse> communicationDataResponse;

    private Long selectedCustomerCommunicationId;

    private List<LiabilitiesOffsettingChoice> checkedCustomerLiabilities;

    private List<LiabilitiesOffsettingChoice> checkedReceivables;

    private List<LiabilitiesOffsettingChoice> checkedDeposits;

    private List<LiabilitiesOffsettingChoice> negativePayments;

    private List<ReceivableTemplateResponse> templateResponses;

    private List<FileWithStatusesResponse> files;

    private LocalDate date;

}
