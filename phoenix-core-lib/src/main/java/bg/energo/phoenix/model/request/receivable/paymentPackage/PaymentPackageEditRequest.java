package bg.energo.phoenix.model.request.receivable.paymentPackage;

import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentPackageEditRequest {

    @NotNull(message = "lockStatus-[lockStatus] should not be null;")
    private PaymentPackageLockStatus lockStatus;

}
