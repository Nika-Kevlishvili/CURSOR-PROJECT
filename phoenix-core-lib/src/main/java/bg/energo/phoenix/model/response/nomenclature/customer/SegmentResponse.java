package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

/**
 * <h1>SegmentResponse object</h1>
 * {@link #id}
 * {@link #name}
 * {@link #orderingId}
 * {@link #defaultSelection}
 * {@link #status}
 * {@link #systemUserId}
 */
@Data
public class SegmentResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public SegmentResponse(Segment segment) {
        this.id = segment.getId();
        this.name = segment.getName();
        this.orderingId = segment.getOrderingId();
        this.defaultSelection = segment.isDefaultSelection();
        this.status = segment.getStatus();
        this.systemUserId = segment.getSystemUserId();
    }
}
