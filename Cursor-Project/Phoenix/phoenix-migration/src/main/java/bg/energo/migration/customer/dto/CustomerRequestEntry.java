package bg.energo.migration.customer.dto;

import bg.energo.migration.integration.phoenix.customer.model.request.CreateCustomerRequest;

public record CustomerRequestEntry(
        String foreignId,
        CreateCustomerRequest request
) {
}
