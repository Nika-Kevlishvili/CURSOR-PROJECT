package phoenix.core.customer.model.response.customer;

import lombok.Data;
import phoenix.core.customer.model.entity.nomenclature.customer.Segment;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

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
