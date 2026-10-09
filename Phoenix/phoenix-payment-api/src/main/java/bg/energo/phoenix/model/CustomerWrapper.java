package bg.energo.phoenix.model;

import bg.energo.phoenix.model.entity.customer.Customer;

public record CustomerWrapper(
        Customer customer,
        String customerShortDetails,
        String billingGroupNumber
) {
}
