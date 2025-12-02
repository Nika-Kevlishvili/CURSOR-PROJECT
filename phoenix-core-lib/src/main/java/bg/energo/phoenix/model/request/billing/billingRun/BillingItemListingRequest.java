package bg.energo.phoenix.model.request.billing.billingRun;

import lombok.Data;

@Data
public class BillingItemListingRequest {
    private Integer size;
    private Integer page;
    private String prompt;

}
