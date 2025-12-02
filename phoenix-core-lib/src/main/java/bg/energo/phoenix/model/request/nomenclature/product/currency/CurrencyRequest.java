package bg.energo.phoenix.model.request.nomenclature.product.currency;

import bg.energo.phoenix.model.customAnotations.product.product.currency.AlternativeCurrencyExchangeRateValidator;
import bg.energo.phoenix.model.customAnotations.product.product.currency.MainCurrencyStartDateValidator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import io.avaje.lang.Nullable;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@AlternativeCurrencyExchangeRateValidator
//@MainCurrencyStartDateAndStatusValidator
@MainCurrencyStartDateValidator
@EqualsAndHashCode(callSuper = false)
public class CurrencyRequest {
    @NotEmpty(message = "name-name should not be empty;")
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length;")
    private String name;

    @NotEmpty(message = "printName-printName should not be empty;")
    @Size(min = 1, max = 512, message = "printName-printName does not match the allowed length;")
    private String printName;

    @NotEmpty(message = "printName-abbreviation should not be empty;")
    @Size(min = 1, max = 128, message = "abbreviation-abbreviation does not match the allowed length;")
    private String abbreviation;

    @NotEmpty(message = "fullName-abbreviation should not be empty;")
    @Size(min = 1, max = 512, message = "fullName-abbreviation does not match the allowed length;")
    private String fullName;

    private Long altCurrencyId;

    @Nullable()
    @DecimalMin(value = "0.0000000001", message = "altCurrencyExchangeRate-altCurrencyExchangeRate should be in range 0.0000000001_9999999999.99;")
    @DecimalMax(value = "9999999999.99", message = "altCurrencyExchangeRate-altCurrencyExchangeRate should be in range 0.0000000001_9999999999.99;")
    private BigDecimal altCurrencyExchangeRate;

    private Boolean mainCurrency = false;

    private LocalDate mainCurrencyStartDate;

    @NotNull(message = "status-status should not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-defaultSelection should not be null;")
    private Boolean defaultSelection = false;
}
