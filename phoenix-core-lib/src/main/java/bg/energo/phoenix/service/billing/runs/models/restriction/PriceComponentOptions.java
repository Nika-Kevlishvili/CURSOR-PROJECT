package bg.energo.phoenix.service.billing.runs.models.restriction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceComponentOptions {
    private Long priceComponentId;
    private Boolean isKwtRestrictionSelected;
    private Boolean isCcyRestrictionSelected;
    private BigDecimal restrictionPercent;
    private List<KwhRestriction> kwhRestrictions;
    private List<CcyRestrictions> ccyRestrictions;
    private Long mainCurrencyId;
    private Long priceComponentCurrencyId;
}
