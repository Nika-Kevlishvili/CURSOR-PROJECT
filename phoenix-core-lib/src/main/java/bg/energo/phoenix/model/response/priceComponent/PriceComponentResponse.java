package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.XEnergieApplicationType;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import lombok.Data;

@Data
public class PriceComponentResponse {
    private Long id;
    private CurrencyResponse currency;
    private VatRateResponse vatRate;
    private String name;
    private String invoiceAndTemplateText;
    private NumberType numberType;
    private Boolean globalVatRate;
    private String incomeAccountNumber;
    private String costCenterControllingOrder;
    private String contractTemplateTag;
    private String priceInWords;
    private String priceFormula;
    private PriceComponentStatus status;
    private IssuedSeparateInvoice issuedSeparateInvoice;
    private String conditions;
    private Long priceComponentGroupDetailId;
    private XEnergieApplicationType xenergieApplicationType;

    public PriceComponentResponse(PriceComponent priceComponent) {
        this.id = priceComponent.getId();
        this.currency = priceComponent.getCurrency() == null ? null : new CurrencyResponse(priceComponent.getCurrency());
        this.vatRate = priceComponent.getVatRate() == null ? null : new VatRateResponse(priceComponent.getVatRate());
        this.name = priceComponent.getName();
        this.invoiceAndTemplateText = priceComponent.getInvoiceAndTemplateText();
        this.numberType = priceComponent.getNumberType();
        this.globalVatRate = priceComponent.getGlobalVatRate();
        this.incomeAccountNumber = priceComponent.getIncomeAccountNumber();
        this.costCenterControllingOrder = priceComponent.getCostCenterControllingOrder();
        this.contractTemplateTag = priceComponent.getContractTemplateTag();
        this.priceInWords = priceComponent.getPriceInWords();
        this.priceFormula = priceComponent.getPriceFormula();
        this.status = priceComponent.getStatus();
        this.issuedSeparateInvoice = priceComponent.getIssuedSeparateInvoice();
        this.conditions = priceComponent.getConditions();
        this.priceComponentGroupDetailId = priceComponent.getPriceComponentGroupDetailId();
        this.xenergieApplicationType = priceComponent.getXenergieApplicationType();
    }
}
