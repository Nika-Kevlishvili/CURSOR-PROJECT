package bg.energo.phoenix.billingRun.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfilePrice {
    //PriceParameterId
    private String p;
    //PriceValue
    private BigDecimal v;
    //IsProfile
    private Boolean b;
}
