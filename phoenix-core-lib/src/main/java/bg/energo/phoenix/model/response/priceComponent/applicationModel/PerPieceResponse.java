package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import lombok.Data;

import java.util.List;

@Data
public class PerPieceResponse {

    List<PriceRangesResponse> ranges;

    public PerPieceResponse(List<PriceRangesResponse> ranges) {
        this.ranges = ranges;
    }
}
