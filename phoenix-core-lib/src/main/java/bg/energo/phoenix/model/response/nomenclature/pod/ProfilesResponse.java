package bg.energo.phoenix.model.response.nomenclature.pod;

import bg.energo.phoenix.model.entity.nomenclature.pod.Profiles;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.time.TimeZone;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfilesResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private Boolean defaultSelection;
    private Boolean isHardCoded;
    private NomenclatureItemStatus status;
    private TimeZone timeZone;

    public ProfilesResponse(Profiles profiles) {
        this.id = profiles.getId();
        this.name = profiles.getName();
        this.orderingId = profiles.getOrderingId();
        this.defaultSelection = profiles.getIsDefault();
        this.isHardCoded = profiles.getIsHardCoded();
        this.status = profiles.getStatus();
        this.timeZone = profiles.getTimeZone();
    }
}