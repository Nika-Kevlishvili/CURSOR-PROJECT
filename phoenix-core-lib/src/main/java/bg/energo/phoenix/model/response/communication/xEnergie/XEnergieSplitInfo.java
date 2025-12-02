package bg.energo.phoenix.model.response.communication.xEnergie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class XEnergieSplitInfo {
    private Long id;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String pointOfDeliveryIdentifier;
    private Long dealId;
    private Boolean deleteAvailable;
}
