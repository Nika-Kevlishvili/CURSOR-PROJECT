package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerViewOwner {
    private Long id;
    private BelongingCapitalOwnerResponse belongingCapitalOwner;
    private CustomerResponse customer;
    private CustomerResponse ownerCustomer;
    private String additionalInfo;
    private Status status;
}
