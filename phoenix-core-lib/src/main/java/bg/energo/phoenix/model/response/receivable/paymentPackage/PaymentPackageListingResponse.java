package bg.energo.phoenix.model.response.receivable.paymentPackage;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PaymentPackageListingResponse {

    private Long id;

    private String collectionChannelName;

    private PaymentPackageLockStatus lockStatus;

    private LocalDate paymentDate;

    private EntityStatus entityStatus;

    public PaymentPackageListingResponse(PaymentPackageListingMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.collectionChannelName = middleResponse.getCollectionChannel();
        this.lockStatus = middleResponse.getStatus();
        this.paymentDate = middleResponse.getPaymentDate();
        this.entityStatus = middleResponse.getEntityStatus();
    }
}
