package bg.energo.phoenix.model.response.terminationGroup;

import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroupDetails;
import lombok.Data;

@Data
public class TerminationGroupShortResponse {
    private Long id;
    private String name;

    public TerminationGroupShortResponse(TerminationGroupDetails details) {
        Long terminationGroupId = details.getTerminationGroupId();
        this.id = terminationGroupId;
        this.name = "%s (%s)".formatted(details.getName(), terminationGroupId);
    }
}
