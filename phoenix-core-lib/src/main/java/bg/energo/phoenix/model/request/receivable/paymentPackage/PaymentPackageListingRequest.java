package bg.energo.phoenix.model.request.receivable.paymentPackage;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageListingType;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageSortingType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class PaymentPackageListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<Long> collectionChannelIds;

    private PaymentPackageListingType searchBy;

    private List<PaymentPackageLockStatus> lockStatuses;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "fromDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "toDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate toDate;

    private PaymentPackageSortingType sortingField;

    private Sort.Direction direction;

}
