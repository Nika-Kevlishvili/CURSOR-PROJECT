package bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice;

import bg.energo.phoenix.model.customAnotations.billing.billingRun.SummaryDataRowVatRateValidator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@SummaryDataRowVatRateValidator
public class SummaryDataRowParameters {

    private Long id;

    @Size(min = 1, max = 1024, message = "priceComponentOrPriceComponentGroupOrItem-[priceComponentOrPriceComponentGroupOrItem] length should be between {min} and {max};")
    private String priceComponentOrPriceComponentGroupOrItem;

    @DecimalMin(value = "-999999999.99999999", message = "totalVolumes-[totalVolumes] minimum value is {value};")
    @DecimalMax(value = "999999999.99999999", message = "totalVolumes-[totalVolumes] maximum value is {value};")
    @Digits(integer = 9, fraction = 8, message = "totalVolumes-totalVolumes Invalid format. totalVolumes should be in range -999999999.99999999 - 999999999.99999999;")
    private BigDecimal totalVolumes;

    @Size(min = 1, max = 512, message = "unitOfMeasuresForTotalVolumes-[unitOfMeasuresForTotalVolumes] should be between {min} and {max};")
    private String unitOfMeasuresForTotalVolumes;

    @DecimalMin(value = "-999999999.9999999999", message = "unitPrice-[unitPrice] minimum value is {value};")
    @DecimalMax(value = "999999999.9999999999", message = "unitPrice-[unitPrice] maximum value is {value};")
    @Digits(integer = 9, fraction = 10, message = "unitPrice-unitPrice Invalid format. unitPrice should be in range -999999999.9999999999 - 999999999.9999999999;")
    private BigDecimal unitPrice;

    @Size(min = 1, max = 512, message = "unitOfMeasureForUnitPrice-[unitOfMeasureForUnitPrice] should be between {min} and {max};")
    private String unitOfMeasureForUnitPrice;

    @DecimalMin(value = "-999999999.9999999999", message = "value-[value] minimum value is {value};")
    @DecimalMax(value = "999999999.9999999999", message = "value-[value] maximum value is {value};")
    @Digits(integer = 9, fraction = 10, message = "value-value Invalid format. value should be in range -999999999.9999999999 - 999999999.9999999999;")
    private BigDecimal value;

    private Long valueCurrencyId;

    @Size(min = 1, max = 32, message = "incomeAccount-[incomeAccount] length should be between {min} and {max};")
    private String incomeAccount;

    @Size(min = 1, max = 32, message = "costCenter-[costCenter] length should be between {min} and {max};")
    private String costCenter;

    private Long vatRateId;

    private Boolean globalVatRate;
}
