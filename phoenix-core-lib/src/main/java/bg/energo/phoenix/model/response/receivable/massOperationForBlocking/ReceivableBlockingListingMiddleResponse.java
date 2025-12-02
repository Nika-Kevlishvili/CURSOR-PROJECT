package bg.energo.phoenix.model.response.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingStatus;

public interface ReceivableBlockingListingMiddleResponse {
    Long getBlockingId();

    String getName();

    String getReasonType();

    ReceivableBlockingStatus getBlockingStatus();

    EntityStatus getEntityStatus();

}
