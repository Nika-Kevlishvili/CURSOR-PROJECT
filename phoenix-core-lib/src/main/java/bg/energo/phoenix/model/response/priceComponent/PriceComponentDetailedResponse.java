package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.XEnergieApplicationType;
import bg.energo.phoenix.model.request.product.price.priceComponent.AlternateInvoiceRecipient;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.PriceComponentPriceTypeResponse;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.PriceComponentValueTypeResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.ApplicationModelResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceComponentDetailedResponse {
    private Long id;
    private PriceComponentPriceTypeResponse priceType;
    private PriceComponentValueTypeResponse valueType;
    private CurrencyResponse currency;
    private VatRateResponse vatRate;
    private String name;
    private String invoiceAndTemplateText;
    private NumberType numberType;
    private Boolean globalVatRate;
    private Boolean discount;
    private String incomeAccountNumber;
    private String costCenterControllingOrder;
    private String contractTemplateTag;
    private String priceInWords;
    private String priceFormula;
    private List<PriceFormulaPreviewInfo> priceFormulaInfo;
    private PriceComponentStatus status;
    private IssuedSeparateInvoice issuedSeparateInvoice;
    private String conditions;
    private List<PriceFormulaPreviewInfo> conditionsInfo;
    private List<FormulaVariablePayload> formulaVariables;
    private ApplicationModelResponse applicationModelResponse;
    private Boolean isLocked;
    private AlternateInvoiceRecipient alternateInvoiceRecipient;
    private Boolean doNotIncludeVatBase;
    private XEnergieApplicationType xenergieApplicationType;

    @Builder
    @Data
    public static class FormulaVariablePayload {
        private Long id;
        private String variable;
        private String description;
        private BigDecimal value;
        private BigDecimal valueFrom;
        private BigDecimal valueTo;
        private ProfileForBalancingShortResponse balancingProfileName;
    }
}
