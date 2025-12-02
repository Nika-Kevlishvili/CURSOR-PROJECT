package bg.energo.phoenix.model.request.receivable.taxForTheGridOperator;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.SupplierType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxForTheGridOperatorRequest {

    @NotNull(message = "gridOperator-[gridOperator] must not be blank;")
    private Long gridOperator;

    @NotNull(message = "supplierType-[supplierType] must not be blank;")
    private SupplierType supplierType;

    @NotNull(message = "documentTemplateId-[documentTemplateId] must not be blank;")
    private Long documentTemplateId;

    @NotNull(message = "emailTemplateId-[emailTemplateId] must not be blank;")
    private Long emailTemplateId;

    @NotBlank(message = "numberOfIncomeAccount-[numberOfIncomeAccount] must not be blank;")
    @Size(min = 1, max = 32, message = "numberOfIncomeAccount-[numberOfIncomeAccount] does not match the allowed length, range: [1-32];")
    private String numberOfIncomeAccount;

    @Size(min = 1, max = 1024, message = "basisForIssuing-[basisForIssuing] does not match the allowed length, range: [1-1024];")
    @Pattern(regexp = "^[A-Za-zа-яА-Я0-9\\s]+$", message = "Only Latin, Cyrillic, digits and spaces are allowed.")
    private String basisForIssuing;

    @Size(min = 1, max = 32, message = "costCenterControllingOrder-[costCenterControllingOrder] does not match the allowed length, range: [1-32];")
    @NotBlank(message = "costCenterControllingOrder-[costCenterControllingOrder] must not be blank;")
    private String costCenterControllingOrder;

    @Size(min = 1, max = 1024, message = "priceComponentOrPriceComponentGroupOrItem-[priceComponentOrPriceComponentGroupOrItem] does not match the allowed length, range: [1-1024];")
    private String priceComponentOrPriceComponentGroupOrItem;

    @NotNull(message = "taxForReconnection-[taxForReconnection] must not be blank;")
    @DecimalMin(value = "0.00", message = "taxForReconnection-[taxForReconnection] Minimum value is 0.00;")
    @Digits(integer = 8, fraction = 2, message = "taxForReconnection-[taxForReconnection] Maximum value is 99999999;")
    private BigDecimal taxForReconnection;

    @NotNull(message = "taxForExpressReconnection-[taxForExpressReconnection] must not be blank;")
    @DecimalMin(value = "0.00", message = "taxForExpressReconnection-[taxForExpressReconnection] Minimum value is 0.00;")
    @Digits(integer = 8, fraction = 2, message = "taxForExpressReconnection-[taxForExpressReconnection] Maximum value is 99999999;")
    private BigDecimal taxForExpressReconnection;

    @NotNull(message = "currency-[currency] must not be blank;")
    private Long currency;

    @NotNull(message = "removeTaxInCancel-[removeTaxInCancel] must not be null;")
    private boolean removeTaxInCancel;

    @NotNull(message = "defaultForPodWithMeasurementTypeSlp-[defaultForPodWithMeasurementTypeSlp] must not be null;")
    private boolean defaultForPodWithMeasurementTypeSlp;

    @NotNull(message = "defaultForPodWithMeasurementTypeBySettlementPeriod-[defaultForPodWithMeasurementTypeBySettlementPeriod] must not be null;")
    private boolean defaultForPodWithMeasurementTypeBySettlementPeriod;

    @NotNull(message = "defaultSelection-[defaultSelection] must not be null;")
    private boolean defaultSelection;

    @NotBlank(message = "disconnectionType-[disconnectionType] must not be blank;")
    @Size(min = 1, max = 1024, message = "disconnectionType-[disconnectionType] does not match the allowed length, range: [1-1024];")
    private String disconnectionType;

    @NotNull(message = "status-[Status] must not be null;")
    private NomenclatureItemStatus status;

}
