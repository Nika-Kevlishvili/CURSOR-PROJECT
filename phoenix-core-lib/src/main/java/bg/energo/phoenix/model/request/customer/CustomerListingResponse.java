package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerListingResponse {
    String identifier;
    CustomerDetailStatus status;
    CustomerType customerType;
    String customerName;
    String customerMiddleName;
    String customerLastName;
    String legalFormShortDescription;
    String displayName; // first name and last name combined
    String canEditAsManager;
    Long economicBranchCiId;
    String economicBranchName;
    Long populatedPlaceId;
    String populatedPlaceName;
    LocalDateTime createDate;
    String unwantedCustomerStatus;
    Long customerId;
    Long customerDetailId;
    CustomerStatus customerStatus;
}