package bg.energo.phoenix.service.product.price.priceParameter;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceParameterImportHelper {
    private BigDecimal price;
    private int rowNum;
    private String time;
}
