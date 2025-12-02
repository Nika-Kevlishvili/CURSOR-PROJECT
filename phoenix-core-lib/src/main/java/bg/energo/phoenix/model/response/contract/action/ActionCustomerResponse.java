package bg.energo.phoenix.model.response.contract.action;

import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;

public record ActionCustomerResponse(
        Long id,
        String identifier,
        CustomerStatus status,
        CustomerType type,
        String name
) {

    public ActionCustomerResponse(Customer customer,
                                  CustomerDetails customerDetails) {
        this(
                customer.getId(),
                customer.getIdentifier(),
                customer.getStatus(),
                customer.getCustomerType(),
                getFormattedCustomerName(customer, customerDetails)
        );
    }

    private static String getFormattedCustomerName(Customer customer, CustomerDetails customerDetails) {
        switch (customer.getCustomerType()) {
            case PRIVATE_CUSTOMER -> {
                if (customerDetails.getMiddleName() == null) {
                    return "%s (%s %s)".formatted(
                            customer.getIdentifier(),
                            customerDetails.getName(),
                            customerDetails.getLastName()
                    );
                } else {
                    return "%s (%s %s %s)".formatted(
                            customer.getIdentifier(),
                            customerDetails.getName(),
                            customerDetails.getMiddleName(),
                            customerDetails.getLastName()
                    );
                }
            }
            case LEGAL_ENTITY -> {
                return "%s (%s)".formatted(customer.getIdentifier(), customerDetails.getName());
            }
            default -> throw new IllegalArgumentException("Unknown customer type: " + customer.getCustomerType());
        }
    }

}
