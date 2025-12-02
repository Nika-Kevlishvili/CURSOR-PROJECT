package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.ApplicationModelType;
import bg.energo.phoenix.model.enums.billing.billings.BillingApplicationLevel;
import bg.energo.phoenix.model.enums.billing.billings.BillingCriteria;
import bg.energo.phoenix.model.enums.billing.billings.BillingEndDate;
import bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionPreviewInfo;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StandardBillingRunParametersResponse {

    private BillingCriteria billingCriteria;

    private BillingApplicationLevel applicationLevel;

    private List<ApplicationModelType> applicationModelType;

    private String customerContractOrPodConditions;

    private String customerContractOrPodList;

    private List<ConditionPreviewInfo> conditionsInfo;

    private LocalDate maxEndDate;

    private BillingEndDate periodicMaxEndDate;

    private Integer periodicMaxEndDateValue;

    private List<ShortResponse> sumFiles;
}
