package bg.energo.phoenix.model.response.receivable.reminder;

import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;

public interface ReminderCustomerComDataMiddleResponse {
    Long getCustomerId();
    Long getCommId();
    Long getCommContactId();
    String getCommContactValue();
    CustomerCommContactTypes getCommContactType();
    Boolean getCommContactSendSms();
    Long getContactPurposeId();
}
