package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.enums.customer.CustomerType;

public record CustomerDetailsShortResponse(Long id, Long customerDetailsId, String customerName, CustomerType customerType,Boolean businessActivity) {
}
