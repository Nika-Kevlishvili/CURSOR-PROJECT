package bg.energo.phoenix.model.response.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.BlockingReason;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingReasonType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockingReasonResponse {
    private Long id;
    private String name;
    private List<ReceivableBlockingReasonType> reasonTypes;
    private Long orderingId;
    private boolean defaultSelection;
    private boolean isHardCoded;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public BlockingReasonResponse(BlockingReason blockingReason) {
        this.id = blockingReason.getId();
        this.name = blockingReason.getName();
        this.reasonTypes = blockingReason.getReasonTypes();
        this.orderingId = blockingReason.getOrderingId();
        this.defaultSelection = blockingReason.isDefaultSelection();
        this.isHardCoded = blockingReason.isHardCoded();
        this.status = blockingReason.getStatus();
        this.systemUserId = blockingReason.getSystemUserId();
    }
}
