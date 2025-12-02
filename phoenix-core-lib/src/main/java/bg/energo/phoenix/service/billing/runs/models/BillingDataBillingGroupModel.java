package bg.energo.phoenix.service.billing.runs.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BillingDataBillingGroupModel {

    private Long billingGroupId;
    private String groupName;
    private Boolean issueSeparateInvoice;

}
