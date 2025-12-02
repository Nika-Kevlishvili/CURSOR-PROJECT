package bg.energo.phoenix.model.response.terminations;

import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableTerminationResponse {

    private Long id;
    private String name;
    private String fullName;

    public static AvailableTerminationResponse responseFromEntity(Termination termination) {
        String fullName = termination.getName() + " (" + termination.getId() + ")";
        return AvailableTerminationResponse.builder()
                .id(termination.getId())
                .name(termination.getName())
                .fullName(fullName)
                .build();

    }

}
