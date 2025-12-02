package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseInterestRateResponse {

    private Long id;
    private String name;
    private LocalDate dateFrom;
    private BigDecimal percentageRate;
    private NomenclatureItemStatus status;
    private Boolean defaultSelection;
    private Long orderingId;

}
