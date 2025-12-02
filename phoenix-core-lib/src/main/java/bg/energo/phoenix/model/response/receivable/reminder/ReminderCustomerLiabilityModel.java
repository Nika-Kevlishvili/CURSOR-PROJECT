package bg.energo.phoenix.model.response.receivable.reminder;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ReminderCustomerLiabilityModel {

    private Long customerId;

    private Long reminderId;

    private BigDecimal liabilityAmount;

    public ReminderCustomerLiabilityModel(ReminderLiabilityProcessingMiddleResponse middleResponse) {
        this.liabilityAmount = middleResponse.getTotalLiabilityAmount();
        this.reminderId = middleResponse.getReminderId();
        this.customerId = middleResponse.getCustomerId();
    }

}
