package bg.energo.phoenix.model.response.contract.serviceContract;

import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import lombok.Data;

import java.util.List;

@Data
public class ServiceContractAdditionalParametersResponse {

    private ServiceContractBankingDetailsResponse bankingDetails;
    private Long interestRateId;
    private String interestRateName;
    private Long campaignId;
    private String campaignName;
    private Long employeeId;
    private String employeeName;
    private List<ServiceContractSubObjectShortResponse> internalIntermediaries;
    private List<ServiceContractSubObjectShortResponse> externalIntermediaries;
    private List<ServiceContractSubObjectShortResponse> assistingEmployees;
    private List<SystemActivityShortResponse> activities;
    private List<TaskShortResponse> tasks;

    public ServiceContractAdditionalParametersResponse(Boolean directDebit,
                                                       Long employeeId,
                                                       String employeeName,
                                                       Long bankId,
                                                       String bankName,
                                                       String bic,
                                                       String iban,
                                                       Long interestRateId,
                                                       String interestRateName,
                                                       Long campaignId,
                                                       String campaignName) {
        this.bankingDetails = new ServiceContractBankingDetailsResponse(directDebit, bankId, bankName, bic, iban);
        this.interestRateId = interestRateId;
        this.interestRateName = interestRateName;
        this.campaignId = campaignId;
        this.campaignName = campaignName;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
    }
}
