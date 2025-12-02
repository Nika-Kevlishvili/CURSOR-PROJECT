package bg.energo.phoenix.model.response.penaltyGroup;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PenaltyGroupVersionResponse {
    Integer id;
    LocalDate startDate;
    LocalDate endDate;
}
