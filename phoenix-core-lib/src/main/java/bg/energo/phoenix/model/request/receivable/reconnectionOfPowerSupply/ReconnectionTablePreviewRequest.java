package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.TableSearchBy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReconnectionTablePreviewRequest {
    private int page;

    private int pageSize;

    private String prompt;

    private TableSearchBy searchBy;
}
