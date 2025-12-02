package bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsValidator;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestsStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.SupplierType;
import bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests.CustomersForDPSResponse;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@DisconnectionPowerSupplyRequestsValidator
public class DPSRequestsBaseRequest {

    @NotNull(message = "supplierType-supplierType can not be null;")
    private SupplierType supplierType;

    private DisconnectionRequestsStatus disconnectionRequestsStatus;

    @NotNull(message = "gridOperatorId-gridOperatorId can not be null;")
    private Long gridOperatorId;

    @NotNull(message = "reasonOfDisconnection-reasonOfDisconnection can not be null;")
    private Long reasonOfDisconnectionId;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "gridOpRequestRegDate-gridOpRequestRegDate can not be null;")
    private LocalDate gridOpRequestRegDate;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate gridOpDisconnectionFeePayDate;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate powerSupplyDisconnectionDate;

    @DecimalMin(value = "0.00", message = "liabilityAmountFrom-Minimum value is {value};")
    @DecimalMax(value = "999999999999999.99999", message = "liabilityAmountFrom-Maximum value is {value};")
    @Digits(integer = 15, fraction = 5, message = "liabilityAmountFrom-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal liabilityAmountFrom;

    @DecimalMin(value = "0.00", message = "liabilityAmountTo-Minimum value is {value};")
    @DecimalMax(value = "999999999999999.99999", message = "liabilityAmountTo-Maximum value is {value};")
    @Digits(integer = 15, fraction = 5, message = "liabilityAmountTo-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal liabilityAmountTo;

    private Long currencyId;

    @NotNull(message = "conditionType-conditionType can not be null;")
    private CustomerConditionType conditionType;

    private String condition;

    @Size(min = 1, max = 1024, message = "listOfCustomers-List of Customers should be between {min} and {max} characters.;")
    private String listOfCustomer;

    @NotNull(message = "reminderForDisconnectionId-reminderForDisconnectionId can not be null;")
    private Long reminderForDisconnectionId;

    private List<CustomersForDPSResponse> pods;

    private Boolean isAllSelected;

    private List<Long> excludePodIds;

    private Boolean podWithHighestConsumption;

    private Set<Long> templateIds;

    @DuplicatedValuesValidator(fieldPath = "files")
    private List<Long> files;
}
