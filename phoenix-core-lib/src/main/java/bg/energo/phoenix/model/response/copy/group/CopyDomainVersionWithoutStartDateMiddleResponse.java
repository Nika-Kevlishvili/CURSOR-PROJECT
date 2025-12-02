package bg.energo.phoenix.model.response.copy.group;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CopyDomainVersionWithoutStartDateMiddleResponse {
    private Long id;
    private Long versionId;
    private String name;
}
