package bg.energo.phoenix.model.response.terminationGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerminationGroupVersion {
    private Long version;
    private LocalDate startDate;
}
