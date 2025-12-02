package bg.energo.phoenix.model.request.customer.customerAccountManager;

import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <h2>CreateCustomerAccountManagerRequest</h2>
 * <b>Variables</b>:<br>
 * <ul>
 *     <li> {@link Long} - {@link #accountManagerId} - Account Manager ID</li>
 *     <li> {@link Long} - {@link #accountManagerTypeId} - Account Manager Type ID</li>
 * </ul>
 * @see bg.energo.phoenix.model.entity.customer.AccountManager AccountManager
 * @see bg.energo.phoenix.model.entity.nomenclature.customer.AccountManagerType AccountManagerType
 */
@NoArgsConstructor
public class CreateCustomerAccountManagerRequest extends BaseCustomerAccountManagerRequest {
    @Builder
    public CreateCustomerAccountManagerRequest(Long accountManagerId, Long accountManagerTypeId) {
        super(accountManagerId, accountManagerTypeId);
    }

    public CreateCustomerAccountManagerRequest(EditCustomerAccountManagerRequest request) {
        this.setAccountManagerId(request.getAccountManagerId());
        this.setAccountManagerTypeId(request.getAccountManagerTypeId());
    }

    public static List<CreateCustomerAccountManagerRequest> getCreateCustomerAccountManagerRequests(List<EditCustomerAccountManagerRequest> editCustomerAccountManagerRequests) {
        List<CreateCustomerAccountManagerRequest> createCustomerAccountManagerRequests = new ArrayList<>();
        if (!CollectionUtils.isEmpty(editCustomerAccountManagerRequests)) {
            for (EditCustomerAccountManagerRequest request : editCustomerAccountManagerRequests) {
                createCustomerAccountManagerRequests.add(new CreateCustomerAccountManagerRequest(request));
            }
        }
        return createCustomerAccountManagerRequests;
    }
}