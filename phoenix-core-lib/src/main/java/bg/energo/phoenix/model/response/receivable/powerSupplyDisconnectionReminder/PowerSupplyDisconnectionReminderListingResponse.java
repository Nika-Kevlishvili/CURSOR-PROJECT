package bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface PowerSupplyDisconnectionReminderListingResponse {

    Long getId();
    String getReminderNumber();
    LocalDateTime getCustomerSendDate();
    String getReminderStatus();
    String getStatus();
    Integer getNumberOFCustomers();
    LocalDate getCreateDate();
    LocalDate getDisconnectionDate();
}
