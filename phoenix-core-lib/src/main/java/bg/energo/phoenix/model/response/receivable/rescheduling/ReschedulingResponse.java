package bg.energo.phoenix.model.response.receivable.rescheduling;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingInterestType;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingStatus;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateShortResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReschedulingResponse {

    private Long id;
    private String reschedulingNumber;
    private Long customerAssessmentId;
    private String customerAssessmentName;
    private ReschedulingStatus reschedulingStatus;
    private EntityStatus status;
    private CustomerDetailsShortResponse customerResponse;
    private Long customerCommunicationDataId;
    private Long customerCommunicationDataIdForContract;
    private List<CustomerCommunicationDataResponse> communicationDataResponseForBilling;
    private List<CustomerCommunicationDataResponse> communicationDataResponseForContract;
    private Integer numberOfInstallment;
    private BigDecimal amountOfTheInstallment;
    private ShortResponse currencyResponse;
    private InterestRateShortResponse InterestRateResponse;
    private InterestRateShortResponse interestRateForInstallmentsResponse;
    private Short installmentDueDay;
    private ReschedulingInterestType interestType;
    private List<TaskShortResponse> reschedulingTasksResponses;
    private List<ReschedulingLiabilityResponse> reschedulingLiabilityResponses;
    private List<ContractTemplateShortResponse> contractTemplateShortResponses;
    private List<ReschedulingFileResponse> files;
    private Boolean reversed;
    private List<ReschedulingPlansResponse> reschedulingPlans;
    private Boolean isLiabilityListChanged;
    private BigDecimal sumOfAmount;
    private BigDecimal sumOfPrincipalAmount;
    private BigDecimal sumOfInterestAmount;
    private BigDecimal sumOfFeeAmount;
}
