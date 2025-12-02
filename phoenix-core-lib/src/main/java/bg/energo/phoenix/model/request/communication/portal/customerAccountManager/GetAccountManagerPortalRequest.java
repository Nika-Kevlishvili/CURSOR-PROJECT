package bg.energo.phoenix.model.request.communication.portal.customerAccountManager;

import bg.energo.phoenix.model.enums.portal.PortalApplicationUserStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * <h2>GetAccountManagerPortalRequest</h2>
 * <b>Variables</b>:<br>
 * <ul>
 *     <li> {@link PortalApplicationUserStatus} - {@link #applicationUserStatus} </li>
 *     <li> {@link String} - {@link #filter} </li>
 * </ul>
 */
@Data
@Builder
public class GetAccountManagerPortalRequest {
    @JsonProperty("userStatus")
    private PortalApplicationUserStatus applicationUserStatus;

    @JsonProperty("filter")
    private String filter;
}
