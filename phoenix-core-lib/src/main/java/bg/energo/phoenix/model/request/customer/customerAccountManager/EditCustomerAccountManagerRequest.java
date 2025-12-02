package bg.energo.phoenix.model.request.customer.customerAccountManager;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <h2>EditCustomerAccountManagerRequest</h2>
 * <b>Variables</b>:<br>
 * <ul>
 *     <li> {@link Long} - {@link #id} - ID</li>
 *     <li> {@link Long} - {@link #accountManagerId} - Account Manager ID</li>
 *     <li> {@link Long} - {@link #accountManagerTypeId} - Account Manager Type ID</li>
 * </ul>
 * @see bg.energo.phoenix.model.entity.customer.CustomerAccountManager CustomerAccountManager
 * @see bg.energo.phoenix.model.entity.customer.AccountManager AccountManager
 * @see bg.energo.phoenix.model.entity.nomenclature.customer.AccountManagerType AccountManagerType
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EditCustomerAccountManagerRequest extends BaseCustomerAccountManagerRequest {
    private Long id;
}