package bg.energo.phoenix.model.response.penalty;

import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailablePenaltyResponse {

    private Long id;
    private String name;
    private String fullName;

    public static AvailablePenaltyResponse responseFromEntity(Penalty penalty) {
        String fullName = penalty.getName() + " (" + penalty.getId() + ")";
        return AvailablePenaltyResponse.builder()
                .id(penalty.getId())
                .name(penalty.getName())
                .fullName(fullName)
                .build();

    }

}
