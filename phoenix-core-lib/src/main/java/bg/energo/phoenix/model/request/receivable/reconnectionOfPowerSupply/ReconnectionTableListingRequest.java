package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.TableSearchBy;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReconnectionTableListingRequest {

    @NotNull(message = "grid operator id is mandatory;")
    private Long gridOperatorId;

    private int page;

    private int pageSize;

    private String prompt;

    private TableSearchBy searchBy;

}
