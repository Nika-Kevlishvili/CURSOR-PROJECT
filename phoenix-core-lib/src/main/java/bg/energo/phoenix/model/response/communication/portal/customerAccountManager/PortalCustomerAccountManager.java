package bg.energo.phoenix.model.response.communication.portal.customerAccountManager;

import bg.energo.phoenix.model.enums.customer.Status;
import lombok.Data;

@Data
public class PortalCustomerAccountManager {
    private String userName;
    private String userFirstName;
    private String userLastName;
    private String userDisplayName;
    private String emailAddress;
    private String userDepartment;
    private Status status;


}
