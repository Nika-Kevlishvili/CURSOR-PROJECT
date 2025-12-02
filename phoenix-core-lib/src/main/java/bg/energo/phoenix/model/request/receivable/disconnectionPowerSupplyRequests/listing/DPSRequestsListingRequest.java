package bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests.listing;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsListingValidator;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestsStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.SupplierType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@DisconnectionPowerSupplyRequestsListingValidator
public class DPSRequestsListingRequest {

    @Size(min = 1, message = "prompt-Prompt length must be 1 or more")
    private String prompt;

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private DPSRequestsListColumns sortBy;

    private DPSRequestsSearchByEnums searchBy;

    private Sort.Direction direction;

    private List<SupplierType> supplierTypes;

    private List<Long> gridOperatorIds;

    private List<DisconnectionRequestsStatus> disconnectionRequestsStatuses;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate gridOperatorRequestRegistrationDateFrom;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate gridOperatorRequestRegistrationDateTo;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate customerReminderLetterSentDateFrom;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate customerReminderLetterSentDateTo;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate gridOperatorDisconnectionFeePayDateFrom;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate gridOperatorDisconnectionFeePayDateTo;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate powerSupplyDisconnectionDateFrom;

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate powerSupplyDisconnectionDateTo;

    private Long numberOfPodsFrom;

    private Long numberOfPodsTo;

}
