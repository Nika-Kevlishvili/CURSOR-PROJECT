package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.BillingApplicationLevel;
import bg.energo.phoenix.model.enums.billing.billings.BillingCriteria;
import bg.energo.phoenix.model.enums.billing.billings.BillingType;

public interface BillingRunListingPeriodicMiddleResponse {

    String getBillingNumber();

    String getRunStages();

    BillingCriteria getBillingCriteria();

    BillingApplicationLevel getApplicationLevel();

    String getProcessPeriodicity();

    Long getBillingId();

    BillingType getType();
}
