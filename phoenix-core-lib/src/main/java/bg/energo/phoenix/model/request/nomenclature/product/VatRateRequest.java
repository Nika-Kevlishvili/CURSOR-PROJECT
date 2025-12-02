package bg.energo.phoenix.model.request.nomenclature.product;

import bg.energo.phoenix.model.customAnotations.product.product.vatRate.GlobalVatRateAndStatusValidator;
import bg.energo.phoenix.model.customAnotations.product.product.vatRate.VatRateStartDateValidator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@VatRateStartDateValidator
@GlobalVatRateAndStatusValidator
public class VatRateRequest {
    @NotBlank(message = "name-name should not be empty;")
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length. allowed length is: 1_512;")
    private String name;

    @NotNull(message = "valueInPercent-valueInPercent should not be null;")
    @DecimalMin(value = "0.00", message = "valueInPercent-valueInPercent should be in range 0.00_999.99;")
    @DecimalMax(value = "999.99", message = "valueInPercent-valueInPercent should be in range 0.01_999.99;")
    @Digits(integer = 3, fraction = 2, message = "valueInPercent-valueInPercent Invalid format. valueInPercent should be in range 0.00_999.99;")
    private BigDecimal valueInPercent;

    private Boolean globalVatRate = false;

    private LocalDate startDate;

    @NotNull(message = "status-status should not be null;")
    private NomenclatureItemStatus status;


}
