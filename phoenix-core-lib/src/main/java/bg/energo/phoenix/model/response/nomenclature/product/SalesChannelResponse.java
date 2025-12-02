package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.customer.PortalTag;
import bg.energo.phoenix.model.entity.nomenclature.product.SalesChannel;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.nomenclature.contract.PortalTagResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h1>SalesChannelResponse Object</h1>
 * {@link #id}
 * {@link #name}
 * {@link #loginPortalTag}
 * {@link #orderingId}
 * {@link #offPremisesContracts}
 * {@link #defaultSelection}
 * {@link #status}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SalesChannelResponse {

    private Long id;
    private String name;
    private PortalTagResponse portalTagResponse;
    private Long orderingId;
    private Boolean offPremisesContracts;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;

    public SalesChannelResponse(SalesChannel salesChannel) {
        this.id = salesChannel.getId();
        this.name = salesChannel.getName();
        this.offPremisesContracts = salesChannel.getOffPremisesContracts();
        this.orderingId = salesChannel.getOrderingId();
        this.defaultSelection = salesChannel.isDefaultSelection();
        this.status = salesChannel.getStatus();
    }

    public SalesChannelResponse(SalesChannel salesChannel, PortalTag portalTag) {
        this.id = salesChannel.getId();
        this.name = salesChannel.getName();
        if (portalTag != null) {
            this.portalTagResponse = new PortalTagResponse(portalTag);
        }
        this.offPremisesContracts = salesChannel.getOffPremisesContracts();
        this.orderingId = salesChannel.getOrderingId();
        this.defaultSelection = salesChannel.isDefaultSelection();
        this.status = salesChannel.getStatus();
    }

}
