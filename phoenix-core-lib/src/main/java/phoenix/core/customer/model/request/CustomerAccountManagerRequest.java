package phoenix.core.customer.model.request;

import lombok.Data;

@Data
public class CustomerAccountManagerRequest {
    private String employeeId; //TODO: Find out if for external api this is enough
    private Long accountManagerTypeId;

}
