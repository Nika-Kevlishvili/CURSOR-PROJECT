package bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

@Data
public class CustomersForDPSRequest {

    @Size(min = 1, message = "prompt-Prompt length must be 1 or more")
    private String prompt;

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private CustomersForDPSListColumns sortBy;

    private CustomersForDPSSearchByEnums searchBy;

    private Sort.Direction direction;

    private Long powerSupplyDisconnectionReminderId;

    private Long gridOperatorId;

    @DecimalMin(value = "0.00", message = "liabilityAmountFrom-Minimum value is {value};")
    @DecimalMax(value = "999999999999999.99999", message = "liabilityAmountFrom-Maximum value is {value};")
    @Digits(integer = 15, fraction = 5, message = "liabilityAmountFrom-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal liabilityAmountFrom;

    @DecimalMin(value = "0.00", message = "liabilityAmountTo-Minimum value is {value};")
    @DecimalMax(value = "999999999999999.99999", message = "liabilityAmountTo-Maximum value is {value};")
    @Digits(integer = 15, fraction = 5, message = "liabilityAmountTo-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal liabilityAmountTo;

    private Boolean isHighestConsumption;

    @NotNull(message = "conditionType-conditionType can not be null;")
    private CustomerConditionType conditionType;

    private String condition;

    @Size(min = 1, max = 1024, message = "listOfCustomers-List of Customers should be between {min} and {max} characters.;")
    private String listOfCustomer;

}
