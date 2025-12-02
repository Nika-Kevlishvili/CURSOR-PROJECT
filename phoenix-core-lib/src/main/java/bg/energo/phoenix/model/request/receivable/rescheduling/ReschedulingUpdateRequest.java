package bg.energo.phoenix.model.request.receivable.rescheduling;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReschedulingUpdateRequest extends ReschedulingRequest {
    private List<Long> liabilityIds;
}
