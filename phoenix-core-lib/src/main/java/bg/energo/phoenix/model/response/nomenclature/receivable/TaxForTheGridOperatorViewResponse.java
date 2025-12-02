package bg.energo.phoenix.model.response.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.TaxesForTheGridOperator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.SupplierType;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxForTheGridOperatorViewResponse {

    private Long id;
    private Long gridOperator;
    private String gridOperatorName;
    private SupplierType supplierType;
    private BigDecimal taxForReconnection;
    private BigDecimal taxForExpressReconnection;
    private Long currency;
    private boolean removeTaxInCancel;
    private boolean defaultForPodWithMeasurementTypeSlp;
    private boolean defaultForPodWithMeasurementTypeBySettlementPeriod;
    private boolean defaultSelection;
    private String disconnectionType;
    private ContractTemplateShortResponse emailTemplateResponse;
    private ContractTemplateShortResponse documentTemplateResponse;
    private String numberOfIncomeAccount;
    private String basisForIssuing;
    private String costCenterControllingOrder;
    private String priceComponentOrPriceComponentGroupOrItem;
    private NomenclatureItemStatus status;

    public TaxForTheGridOperatorViewResponse(TaxesForTheGridOperator tax, GridOperator gridOperator) {
        this.id = tax.getId();
        this.gridOperator = tax.getGridOperator();
        this.gridOperatorName = gridOperator.getName();
        this.supplierType = tax.getSupplierType();
        this.taxForReconnection = tax.getTaxForReconnection();
        this.taxForExpressReconnection = tax.getTaxForReconnectionExpress();
        this.currency = tax.getCurrency();
        this.removeTaxInCancel = tax.isRemoveTaxInCancel();
        this.defaultForPodWithMeasurementTypeSlp = tax.isDefaultForPodWithMeasurementTypeSlp();
        this.defaultForPodWithMeasurementTypeBySettlementPeriod = tax.isDefaultForPodWithMeasurementTypeBySettlementPeriod();
        this.defaultSelection = tax.isDefaultSelection();
        this.disconnectionType = tax.getDisconnectionType();
        this.status = tax.getStatus();
        this.numberOfIncomeAccount=tax.getNumberOfIncomeAccount();
        this.basisForIssuing = tax.getBasisForIssuing();
        this.costCenterControllingOrder = tax.getCostCenterControllingOrder();
        this.priceComponentOrPriceComponentGroupOrItem = tax.getPriceComponentOrPriceComponentGroupOrItem();
    }

}
