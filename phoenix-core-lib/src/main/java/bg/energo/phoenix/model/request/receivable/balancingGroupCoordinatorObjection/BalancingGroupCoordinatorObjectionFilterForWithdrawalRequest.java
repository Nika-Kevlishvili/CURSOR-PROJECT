package bg.energo.phoenix.model.request.receivable.balancingGroupCoordinatorObjection;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class BalancingGroupCoordinatorObjectionFilterForWithdrawalRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private Sort.Direction direction;

}
