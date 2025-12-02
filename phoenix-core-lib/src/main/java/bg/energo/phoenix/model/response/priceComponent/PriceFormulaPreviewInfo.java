package bg.energo.phoenix.model.response.priceComponent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceFormulaPreviewInfo {
    private Long id;
    private String name;
    private String key;
}
