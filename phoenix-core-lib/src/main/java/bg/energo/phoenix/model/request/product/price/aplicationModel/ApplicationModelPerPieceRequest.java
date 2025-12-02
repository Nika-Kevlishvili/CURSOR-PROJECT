package bg.energo.phoenix.model.request.product.price.aplicationModel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
@Data
public class ApplicationModelPerPieceRequest {
    @NotNull(message = "applicationModelRequest.perPieceRequest.ranges-Ranges Can not be null;")
    @Size(min = 1,message = "applicationModelRequest.perPieceRequest.ranges-Ranges can not be empty!;")
    private List<@Valid SettlementPeriodRange> ranges;
}
