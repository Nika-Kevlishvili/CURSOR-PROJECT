package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.ReconnectionStatus;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Collections;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ReconnectionOfThePowerSupplyBaseRequestValidator
public class ReconnectionOfThePowerSupplyBaseRequest {

    @NotNull(message = "gridOperatorId-[gridOperatorId] grid operator id is mandatory!;")
    private Long gridOperatorId;

    private Set<Long> fileIds = Collections.emptySet();

    @NotNull(message = "saveAs-[saveAs] save as is mandatory!;")
    private ReconnectionStatus saveAs;

    private Set<ReconnectionPodRequest> table = Collections.emptySet();

    private Set<Long> templateIds;

    @JsonSetter
    public void setFileIds(Set<Long> fileIds) {
        this.fileIds=fileIds!=null ? fileIds : Collections.emptySet();
    }

    @JsonSetter
    public void setTable(Set<ReconnectionPodRequest> table) {
        this.table=table!=null ? table : Collections.emptySet();
    }
}
