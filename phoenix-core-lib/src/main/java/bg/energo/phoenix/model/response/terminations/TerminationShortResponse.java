package bg.energo.phoenix.model.response.terminations;

import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import lombok.Data;

@Data
public class TerminationShortResponse {
    private Long id;
    private String name;

    public TerminationShortResponse(Termination termination) {
        this.id = termination.getId();
        this.name = "%s (%s)".formatted(termination.getName(), termination.getId());
    }
}
