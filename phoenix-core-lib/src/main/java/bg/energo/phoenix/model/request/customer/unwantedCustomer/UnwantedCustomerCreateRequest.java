package bg.energo.phoenix.model.request.customer.unwantedCustomer;

import bg.energo.phoenix.model.customAnotations.customer.unwantedCustomer.UnwantedCustomerUICValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Param {@link #identificationNumber} customer identification number
 * @Param {@link #name} customer name
 * @Param {@link #unwantedCustomerReasonId} unwanted customer nomenclature reasonId
 * @Param {@link #additionalInfo} string for additional info
 * @Param {@link #contractCreateRestriction} boolean value for contract creation restriction
 * @Param {@link #orderCreateRestriction} boolean value for order creation restriction
 */
@Data
public class UnwantedCustomerCreateRequest {

    @NotBlank(message = "identificationNumber-Identification number shouldn't be blank;")
    @Size(min = 1, max = 32, message = "identificationNumber-Identification number length should be between {min}:{max};")
    @UnwantedCustomerUICValidator(message = "identificationNumber-Identification number is not valid;")
    private String identificationNumber;

    @NotBlank(message = "name-Name shouldn't be blank;")
    @Size(min = 1, max = 2048, message = "name- Name length should be between {min}:{max};")
    @Pattern(regexp = "^[А-ЯA-Z0-9\\d–@#$&*()+\\-:.,'‘€№=\\s]*$", message = "name-Name format is invalid;")
    private String name;

    @NotNull(message = "unwantedCustomerReasonId-Unwanted customer reason ID shouldn't be empty;")
    private Long unwantedCustomerReasonId;

    @Size(min = 1, max = 2048, message = "additionalInfo-Additional information length should be between {min}:{max} characters;")
    @Pattern(regexp = "[\\\\/А-Яа-яA-Za-z0-9–@#$&*()-+\\-_§?!<>:.,'‘€№= ]*$", message = "additionalInfo-Additional information format is invalid;")
    private String additionalInfo;

    @NotNull(message = "contractCreateRestriction-Contract create restriction shouldn't be null;")
    private Boolean contractCreateRestriction;

    @NotNull(message = "orderCreateRestriction-Order create restriction shouldn't be null;")
    private Boolean orderCreateRestriction;

}
