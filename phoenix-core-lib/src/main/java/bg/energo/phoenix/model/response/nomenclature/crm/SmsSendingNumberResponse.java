package bg.energo.phoenix.model.response.nomenclature.crm;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class SmsSendingNumberResponse extends NomenclatureResponse {

    private String smsNumber;
    private Boolean isHardCoded;

    public SmsSendingNumberResponse(Long id, String name, Long orderingId, Boolean defaultSelection, NomenclatureItemStatus status, String smsNumber, Boolean isHardCoded) {
        super(id, name, orderingId, defaultSelection, status);
        this.smsNumber = smsNumber;
        this.isHardCoded = isHardCoded;
    }
}
