package bg.energo.phoenix.model.response.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingType;
import bg.energo.phoenix.util.epb.EPBListUtils;
import lombok.Data;

import java.util.List;

@Data
public class ReceivableBlockingListingResponse {

    private Long id;
    private String name;
    private EntityStatus entityStatus;
    private ReceivableBlockingStatus blockingStatus;
    private List<ReceivableBlockingType> reasonTypes;

    public ReceivableBlockingListingResponse(ReceivableBlockingListingMiddleResponse middleResponse) {
        this.id = middleResponse.getBlockingId();
        this.name = middleResponse.getName();
        this.entityStatus = middleResponse.getEntityStatus();
        this.blockingStatus = middleResponse.getBlockingStatus();
        this.reasonTypes = EPBListUtils.convertDBEnumStringArrayIntoListEnum(ReceivableBlockingType.class, middleResponse.getReasonType());
    }
}
