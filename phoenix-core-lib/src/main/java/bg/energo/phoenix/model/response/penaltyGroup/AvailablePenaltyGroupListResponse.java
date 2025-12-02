package bg.energo.phoenix.model.response.penaltyGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailablePenaltyGroupListResponse {
    private Long id;
    private String name;
    private String displayName;
    private LocalDateTime dateOfCreation;

    public AvailablePenaltyGroupListResponse(Long id, String name, LocalDateTime createDate) {
        this.id = id;
        this.name = name;
        this.displayName = "%s (%s)".formatted(this.name, this.id.toString());
        this.dateOfCreation = createDate;
    }
}
