package bg.energo.phoenix.model.response.contract.productContract;

import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Data
public class AdditionalParametersResponse {

    private String dealNumber;
    private BigDecimal estimatedTotalConsumptionUnderContractKwh;
    private ProductContractBankingDetailsResponse bankingDetails;
    private String riskAssessment;
    private List<String> riskAssessmentAdditionalConditions;
    private Long interestRateId;
    private String interestRateName;
    private Long campaignId;
    private String campaignName;
    private Long employeeId;
    private String employeeName;
    private List<ProductContractSubObjectShortResponse> internalIntermediaries;
    private List<ProductContractSubObjectShortResponse> externalIntermediaries;
    private List<ProductContractSubObjectShortResponse> assistingEmployees;
    private List<SystemActivityShortResponse> activities;
    private List<TaskShortResponse> tasks;

    public AdditionalParametersResponse(String dealNumber,
                                        BigDecimal estimatedTotalConsumptionUnderContractKwh,
                                        Boolean directDebit,
                                        Long employeeId,
                                        String employeeName,
                                        Long bankId,
                                        String bankName,
                                        String bic,
                                        String iban,
                                        String riskAssessment,
                                        String riskAssessmentAdditionalConditions,
                                        Long interestRateId,
                                        String interestRateName,
                                        Long campaignId,
                                        String campaignName) {
        this.dealNumber = dealNumber;
        this.estimatedTotalConsumptionUnderContractKwh = estimatedTotalConsumptionUnderContractKwh;
        this.bankingDetails = new ProductContractBankingDetailsResponse(directDebit, bankId, bankName, bic, iban);
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.riskAssessment = riskAssessment;
        this.riskAssessmentAdditionalConditions = StringUtils.isEmpty(riskAssessmentAdditionalConditions)
                ? null // additional conditions may be empty
                : riskAssessmentAdditionalConditions.contains(";")
                ? Arrays.stream(riskAssessmentAdditionalConditions.split(";")).toList() // or contain multiple statements
                : List.of(riskAssessmentAdditionalConditions); // or contain a single statement
        this.interestRateId = interestRateId;
        this.interestRateName = interestRateName;
        this.campaignId = campaignId;
        this.campaignName = campaignName;
    }
}
