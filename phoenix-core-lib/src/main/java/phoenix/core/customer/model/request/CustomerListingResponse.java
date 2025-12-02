package phoenix.core.customer.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.enums.customer.CustomerStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerListingResponse {
    String identifier;
    CustomerStatus status;
    CustomerType customerType;
    String name;
    //List<String> AccountManager;
    Long economicBranchCiId;
    String economicBranchName;
    Long populatedPlaceId;
    String populatedPlaceName;
    LocalDateTime createDate;
   // UnwantedCustomerStatus unwantedCustomerStatus;
}
