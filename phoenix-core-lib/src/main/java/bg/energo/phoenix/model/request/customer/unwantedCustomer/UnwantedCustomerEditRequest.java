package bg.energo.phoenix.model.request.customer.unwantedCustomer;

import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * <h1>EditCustomerCreateRequest</h1>
 * {@link #name} name of unwanted customer
 * {@link #unwantedCustomerReasonId} unwantedCustomerReason nomenclature id
 * {@link #additionalInfo} String additional info
 * {@link #contractCreateRestriction} Contract create restriction Boolean value
 * {@link #orderCreateRestriction} Order create restriction Boolean value
 * {@link #unwantedCustomerStatus} Unwanted customer status
 */
@Data
public class UnwantedCustomerEditRequest {

    @Size(min = 1, max = 2048, message = "name-Name length should be between {min}:{max} characters;")
    @NotBlank(message = "name-Name shouldn't be blank;")
    @Pattern(regexp = "^[А-Яа-яA-Za-z0-9\\d–@#$&*()+\\-:.,'‘€№=\\s]*$", message = "name-Name format is invalid;")
    private String name;

    @NotNull(message = "unwantedCustomerReasonId-Unwanted customer reason ID shouldn't be null;")
    private Long unwantedCustomerReasonId;

    @Size(min = 1, max = 2048, message = "additionalInfo-Additional information length should be between {min}:{max} characters;")
    @Pattern(regexp = "[\\\\/А-Яа-яA-Za-z0-9–@#$&*()-+\\-_§?!<>:.,'‘€№= ]*$", message = "additionalInfo-Additional information format is invalid;")
    private String additionalInfo;

    @NotNull(message = "contractCreateRestriction-Contract create restriction shouldn't be null;")
    private Boolean contractCreateRestriction;

    @NotNull(message = "orderCreateRestriction-Order create restriction shouldn't be null;")
    private Boolean orderCreateRestriction;

    @NotNull(message = "unwantedCustomerStatus-Unwanted customer status shouldn't be null;")
    private UnwantedCustomerStatus unwantedCustomerStatus;

}
