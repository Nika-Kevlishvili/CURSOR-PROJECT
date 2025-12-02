package bg.energo.phoenix.model.request.product.penalty.penaltyGroup;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchAvailablePenaltyGroupRequest {
    @NotNull(message = "page- Page shouldn't be null;")
    private Integer page;
    @NotNull(message = "size- Size shouldn't be null;")
    private Integer size;
    private String prompt;
}
