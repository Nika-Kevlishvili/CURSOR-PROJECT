package bg.energo.phoenix.model.response.nomenclature.crm;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class TopicOfCommunicationResponse extends NomenclatureResponse {

    private Boolean isHardCoded;

    public TopicOfCommunicationResponse(Long id, String name, Long orderingId, Boolean defaultSelection, NomenclatureItemStatus status, Boolean isHardCoded) {
        super(id, name, orderingId, defaultSelection, status);
        this.isHardCoded = isHardCoded;
    }
}
