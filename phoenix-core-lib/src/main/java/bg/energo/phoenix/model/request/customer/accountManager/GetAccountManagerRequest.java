package bg.energo.phoenix.model.request.customer.accountManager;

import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <h2>GetAccountManagerRequest</h2>
 * <b>Variables</b>:<br>
 * <ul>
 *     <li> {@link String} - {@link #prompt} - searching keyword (search will depend on prompt keyword, searching by
 *     {@link bg.energo.phoenix.model.entity.customer.AccountManager AccountManager}
 *     <ul>
 *         <li>userName</li>
 *         <li>firstName</li>
 *         <li>lastName</li>
 *     </ul>
 *     <li> {@link List<Status> List&lt;Status&gt;} - list of {@link Status} of searching account managers </li>
 *     <li> {@link Integer} - {@link #page} - page index (starting from 0)</li>
 *     <li> {@link Integer} - {@link #size} - page size</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAccountManagerRequest {

    private String prompt;

    @NotEmpty(message = "statuses-Get account managers statuses must not be empty;")
    @NotNull(message = "statuses-Get account managers statuses must not be null;")
    private List<Status> statuses;

    @NotNull(message = "page-Get account managers page must not be null;")
    private Integer page;

    @NotNull(message = "size-Get account managers size must not be null;")
    private Integer size;

}
