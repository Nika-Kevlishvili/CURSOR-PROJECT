package bg.energo.phoenix.model.response.receivable.reminder;

import java.math.BigDecimal;

public interface ReminderLiabilityProcessingMiddleResponse {

    Long getReminderId();

    Long getContactPurposeId();

    Long getCustomerId();

    BigDecimal getTotalLiabilityAmount();

    Long getLiabilityId();

    String getCommunicationChannel();

}
