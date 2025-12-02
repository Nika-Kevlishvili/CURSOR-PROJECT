package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationLevel;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PriceComponentForServiceOrderResponseImpl {

    private Long id;

    private IssuedSeparateInvoice issuedSeparateInvoice;

    private String priceFormula;

    private Long priceComponentValueTypeId;

    private Long priceComponentPriceTypeId;

    private String incomeAccountNumber;

    private String costCenterControllingOrder;

    private Long serviceDetailId;

    private Long vatRateId;

    private Double vatRatePercent;

    private Long applicationModelId;

    private ApplicationModelType applicationModelType;

    private ApplicationType applicationType;

    private String conditions;

    private Long pcGroupDetailId;

    private Long serviceUnitId;

    private String perPieceRanges;

    private ApplicationLevel applicationLevel;

    private NumberType numberType;

    private Integer podQuantityToMultiplyPrice;

    private Boolean noPodCondition;

    private Long currencyId;

    private Long valueTypeId;

    private List<Long> podIds = new ArrayList<>();

    private List<String> unrecognizedPods = new ArrayList<>();

    public PriceComponentForServiceOrderResponseImpl(PriceComponentForServiceOrderResponse response) {
        this.id = response.getId();
        this.issuedSeparateInvoice = response.getIssuedSeparateInvoice();
        this.priceFormula = response.getPriceFormula();
        this.priceComponentValueTypeId = response.getPriceComponentValueTypeId();
        this.priceComponentPriceTypeId = response.getPriceComponentPriceTypeId();
        this.incomeAccountNumber = response.getIncomeAccountNumber();
        this.costCenterControllingOrder = response.getCostCenterControllingOrder();
        this.serviceDetailId = response.getServiceDetailId();
        this.vatRateId = response.getVatRateId();
        this.vatRatePercent = response.getVatRatePercent();
        this.applicationModelId = response.getApplicationModelId();
        this.applicationModelType = response.getApplicationModelType();
        this.applicationType = response.getApplicationType();
        this.conditions = response.getConditions();
        this.pcGroupDetailId = response.getPcGroupDetailId();
        this.serviceUnitId = response.getServiceUnitId();
        this.perPieceRanges = response.getPerPieceRanges();
        this.applicationLevel = response.getApplicationLevel();
        this.numberType = response.getNumberType();
        this.noPodCondition = response.getNoPodCondition();
        this.currencyId = response.getCurrencyId();
        this.valueTypeId = response.getValueTypeId();
    }
}
