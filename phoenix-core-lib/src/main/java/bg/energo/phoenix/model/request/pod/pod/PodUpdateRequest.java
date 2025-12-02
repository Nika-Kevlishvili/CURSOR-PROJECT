package bg.energo.phoenix.model.request.pod.pod;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PodUpdateRequest extends PodBaseRequest {
    private boolean updateExistingVersion;
    private Integer versionId;
}
