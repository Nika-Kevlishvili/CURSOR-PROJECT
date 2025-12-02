package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.util.epb.EPBListUtils;
import lombok.Data;

import java.util.List;

@Data
public class BillingRunListingResponse {

    private Long id;

    private String number;

    private List<RunStage> runStages;

    private BillingCriteria billingCriteria;

    private BillingApplicationLevel applicationLevel;

    private BillingType billingType;

    private BillingStatus status;

    private String accountingPeriod;

    private ExecutionType typeOfPerformance;

    public BillingRunListingResponse(BillingRunListingMiddleResponse middleResponse) {
        this.id = middleResponse.getBillingId();
        this.number = middleResponse.getBillingNumber();
        this.runStages = EPBListUtils.convertDBEnumStringArrayIntoListEnum(RunStage.class, middleResponse.getRunStage());
        this.billingCriteria = middleResponse.getBillingCriteria();
        this.applicationLevel = middleResponse.getApplicationLevel();
        this.billingType = middleResponse.getBillingType();
        this.status = middleResponse.getStatus();
        this.accountingPeriod = middleResponse.getAccountingPeriod();
        this.typeOfPerformance = middleResponse.getExecutionType();
    }
}
