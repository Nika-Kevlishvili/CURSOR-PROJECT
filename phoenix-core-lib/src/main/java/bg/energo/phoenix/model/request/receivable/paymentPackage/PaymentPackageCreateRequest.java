package bg.energo.phoenix.model.request.receivable.paymentPackage;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class PaymentPackageCreateRequest {
    @NotNull(message = "lockStatus-[lockStatus] should not be null;")
    private PaymentPackageLockStatus lockStatus;

    @NotNull(message = "channelId-[channelId] should not be null;")
    private Long channelId;

    @NotNull(message = "accountingPeriodId-[accountingPeriodId] should not be null;")
    private Long accountingPeriodId;

    @DateRangeValidator(fieldPath = "paymentDate", fromDate = "1990-01-01", toDate = "2090-12-31", includedDate = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "paymentDate must not be null;")
    private LocalDate paymentDate;

    private PaymentPackageType type;
}
