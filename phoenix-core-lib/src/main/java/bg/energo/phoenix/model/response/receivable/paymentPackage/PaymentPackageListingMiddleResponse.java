package bg.energo.phoenix.model.response.receivable.paymentPackage;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;

import java.time.LocalDate;

public interface PaymentPackageListingMiddleResponse {

    Long getId();

    String getCollectionChannel();

    PaymentPackageLockStatus getStatus();

    LocalDate getPaymentDate();

    EntityStatus getEntityStatus();

}
