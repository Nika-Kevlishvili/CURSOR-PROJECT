package bg.energo.phoenix.model.response.nomenclature.product.currency;

import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyShortResponse {
    private Long id;
    private String name;

    public CurrencyShortResponse(Currency currency) {
        this.id = currency.getId();
        this.name = currency.getName();
    }
}
