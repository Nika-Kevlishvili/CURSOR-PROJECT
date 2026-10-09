package bg.energo.migration.integration.phoenix.customer.model.response;

import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerStatus;
import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerType;
import bg.energo.migration.integration.phoenix.customer.model.enums.NomenclatureItemStatus;
import bg.energo.migration.integration.phoenix.customer.model.enums.Status;

import java.util.List;

public record CustomerResponse(
        Long id,
        Long customerNumber,
        String identifier,
        CustomerType customerType,
        List<CustomerViewOwner> customerOwners,
        Long lastCustomerDetailId,
        CustomerStatus isDeleted,
        Long version
) {
    public record CustomerViewOwner(
            Long id,
            BelongingCapitalOwner belongingCapitalOwner,
            CustomerResponse customer,
            CustomerResponse ownerCustomer,
            String additionalInfo,
            Status status
    ) {
        public record BelongingCapitalOwner(
                Long id,
                String name,
                NomenclatureItemStatus status,
                Long orderingId,
                boolean defaultSelection,
                String systemUserId
        ) {
        }
    }

}
