package bg.energo.phoenix.model.response.terminationGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerminationGroupTerminationResponse {
    private Long terminationGroupTerminationId;
    private Long terminationId;
    private String terminationName;
}
