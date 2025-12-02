package bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderCustomers;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BillingGroupDataForPSDR {
    private final List<PowerSupplyDisconnectionReminderCustomers> customers = new ArrayList<>();
    private final Long customerCommunicationsId;
    @Setter
    private List<CustomerCommunicationContacts> contacts;

    public BillingGroupDataForPSDR(Long customerCommunicationsId) {
        this.customerCommunicationsId = customerCommunicationsId;
    }

    public void addCustomer(PowerSupplyDisconnectionReminderCustomers customer) {
        customers.add(customer);
    }

}
