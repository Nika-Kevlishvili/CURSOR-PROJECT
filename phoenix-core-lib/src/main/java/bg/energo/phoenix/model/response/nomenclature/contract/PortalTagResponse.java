package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.entity.customer.PortalTag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortalTagResponse {
    private Long id;
    private String portalId;;
    private String name;
    private String nameBg;

    public PortalTagResponse(PortalTag byId) {
        this.id=byId.getId();
        this.portalId= byId.getPortalId();
        this.name=byId.getName();
        this.nameBg=byId.getNameBg();
    }
}
