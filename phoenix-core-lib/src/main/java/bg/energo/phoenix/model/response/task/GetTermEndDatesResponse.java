package bg.energo.phoenix.model.response.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetTermEndDatesResponse {
    private Integer stage;
    private LocalDate endDate;

}
