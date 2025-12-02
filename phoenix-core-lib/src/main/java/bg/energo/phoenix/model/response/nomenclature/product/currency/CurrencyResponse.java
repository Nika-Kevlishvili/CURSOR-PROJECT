package bg.energo.phoenix.model.response.nomenclature.product.currency;

import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyResponse {
    private Long id;
    private String name;
    private String printName;
    private String abbreviation;
    private String fullName;
    private Long altCurrencyId;
    private String altCurrencyName;
    private BigDecimal altCurrencyExchangeRate;
    private Boolean mainCurrency;
    private LocalDate mainCurrencyStartDate;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;



    public CurrencyResponse(Currency currency){
        this.id = currency.getId();
        this.name = currency.getName();
        this.printName = currency.getPrintName();
        this.abbreviation = currency.getAbbreviation();
        this.fullName = currency.getFullName();
        if (currency.getAltCurrency() != null){
            this.altCurrencyId = currency.getAltCurrency().getId();
            this.altCurrencyName = currency.getAltCurrency().getName();
            this.altCurrencyExchangeRate = currency.getAltCurrencyExchangeRate();
        }
        this.mainCurrency = currency.getMainCurrency();
        this.mainCurrencyStartDate = currency.getMainCurrencyStartDate();
        this.orderingId = currency.getOrderingId();
        this.defaultSelection = currency.isDefaultSelection();
        this.status = currency.getStatus();
    }
}
