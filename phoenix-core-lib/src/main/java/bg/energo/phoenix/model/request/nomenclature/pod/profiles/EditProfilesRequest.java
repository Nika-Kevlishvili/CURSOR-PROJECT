package bg.energo.phoenix.model.request.nomenclature.pod.profiles;

import bg.energo.phoenix.model.enums.time.TimeZone;
import lombok.Data;

@Data
public class EditProfilesRequest extends BaseProfilesRequest {
    private TimeZone timeZone;
}
