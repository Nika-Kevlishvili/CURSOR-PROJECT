package bg.energo.phoenix.model.response.communication.xEnergie;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class XEnergieDealInformation {
    private String dealNumber;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
