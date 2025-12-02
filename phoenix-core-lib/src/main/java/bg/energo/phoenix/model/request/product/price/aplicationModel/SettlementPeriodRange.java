package bg.energo.phoenix.model.request.product.price.aplicationModel;

import bg.energo.phoenix.model.customAnotations.product.applicationModel.SettlementPeriodRangeValidator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SettlementPeriodRangeValidator
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettlementPeriodRange {
    @Min(value = 0,message = "perPieceRequest.ranges.from-Should be greater than 0!;")
    @NotNull(message = "perPieceRequest.ranges.from-Range [from] should not be null;")
    private Integer from;
    @Min(value = 0,message = "perPieceRequest.ranges.to-Should be greater than 0!;")
    @NotNull(message = "perPieceRequest.ranges.to-Range [to] Should not be null;")
    private Integer to;
}
