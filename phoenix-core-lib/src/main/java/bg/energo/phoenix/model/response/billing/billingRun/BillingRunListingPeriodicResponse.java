package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.BillingApplicationLevel;
import bg.energo.phoenix.model.enums.billing.billings.BillingCriteria;
import bg.energo.phoenix.model.enums.billing.billings.BillingType;
import bg.energo.phoenix.model.enums.billing.billings.RunStage;
import bg.energo.phoenix.util.epb.EPBListUtils;
import lombok.Data;

import java.util.List;

@Data
public class BillingRunListingPeriodicResponse {

    private Long id;

    private String number;

    private List<RunStage> runStages;

    private BillingCriteria billingCriteria;
    private BillingType billingType;

    private BillingApplicationLevel applicationLevel;

    private String processPeriodicity;

    public BillingRunListingPeriodicResponse(BillingRunListingPeriodicMiddleResponse middleResponse) {
        this.id = middleResponse.getBillingId();
        this.number = middleResponse.getBillingNumber();
        this.runStages = EPBListUtils.convertDBEnumStringArrayIntoListEnum(RunStage.class, middleResponse.getRunStages());
        this.billingCriteria = middleResponse.getBillingCriteria();
        this.applicationLevel = middleResponse.getApplicationLevel();
        this.processPeriodicity = middleResponse.getProcessPeriodicity();
        this.billingType=middleResponse.getType();
    }
}
