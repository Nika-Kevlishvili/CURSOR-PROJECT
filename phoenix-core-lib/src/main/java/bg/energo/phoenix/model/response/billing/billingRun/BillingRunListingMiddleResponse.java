package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.*;

public interface BillingRunListingMiddleResponse {
     Long getBillingId();

     String getBillingNumber();

     String getRunStage();

     BillingCriteria getBillingCriteria();

     BillingApplicationLevel getApplicationLevel();

     BillingType getBillingType();

     BillingStatus getStatus();

     String getAccountingPeriod();

     ExecutionType getExecutionType();
}
