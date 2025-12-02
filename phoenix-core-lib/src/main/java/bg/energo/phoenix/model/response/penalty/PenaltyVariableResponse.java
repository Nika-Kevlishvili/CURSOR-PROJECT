package bg.energo.phoenix.model.response.penalty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyVariableResponse {
    private Long id;
    private String name;
    private String variableName;

    public PenaltyVariableResponse(String name, String variableName) {
        this.name = name;
        this.variableName = variableName;
    }
}
