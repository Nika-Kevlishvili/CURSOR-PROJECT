package bg.energo.phoenix.model.response.communication.portal.customerAccountManager;

import bg.energo.phoenix.model.request.communication.portal.customerAccountManager.GetAccountManagerPortalRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * <h2>GetAccountManagerPortalResponse</h2>
 * <b>Variables</b>:<br>
 * <ul>
 *     <li> {@link GetAccountManagerPortalRequest} - {@link #getAccountManagerPortalRequest} </li>
 *     <li> {@linkplain List<PortalCustomerAccountManager> List&lt;PortalCustomerAccountManager&gt;} - {@link #portalCustomerAccountManagers} </li>
 * </ul>
 */
@Data
public class GetAccountManagerPortalResponse {
    @JsonProperty("request")
    private GetAccountManagerPortalRequest getAccountManagerPortalRequest;

    @JsonProperty("users")
    private List<PortalCustomerAccountManager> portalCustomerAccountManagers;
}
