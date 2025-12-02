package bg.energo.phoenix.model.request.nomenclature.pod.profiles;

import bg.energo.phoenix.model.enums.time.TimeZone;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProfilesRequest extends BaseProfilesRequest {
    @NotNull(message = "timeZone-timezone must not be null;")
    private TimeZone timeZone;
}
