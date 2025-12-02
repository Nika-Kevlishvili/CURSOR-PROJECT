package bg.energo.phoenix.model.response.penalty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyActionTypeResponse {
    private Long id;
    private String name;
}
