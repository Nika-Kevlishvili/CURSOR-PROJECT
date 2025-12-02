package bg.energo.phoenix.model.response.receivable.paymentPackage;

import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackageStatusChangeHistory;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record PaymentPackageStatusChangeHistoryShortResponse(
        Long id,
        PaymentPackageLockStatus lockStatus,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime createDate

) {
    public PaymentPackageStatusChangeHistoryShortResponse(PaymentPackageStatusChangeHistory history) {
        this(history.getId(), history.getLockStatus(), history.getCreateDate());
    }
}
