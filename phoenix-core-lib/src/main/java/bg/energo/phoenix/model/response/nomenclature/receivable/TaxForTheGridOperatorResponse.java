package bg.energo.phoenix.model.response.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.TaxesForTheGridOperator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.SupplierType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxForTheGridOperatorResponse {

    private Long id;
    private Long gridOperator;
    private SupplierType supplierType;
    private BigDecimal taxForReconnection;
    private BigDecimal taxForExpressReconnection;
    private Long currency;
    private boolean removeTaxInCancel;
    private boolean defaultForPodWithMeasurementTypeSlp;
    private boolean defaultForPodWithMeasurementTypeBySettlementPeriod;
    private boolean defaultSelection;
    private String disconnectionType;
    private NomenclatureItemStatus status;

    public TaxForTheGridOperatorResponse(TaxesForTheGridOperator tax) {
        this.id = tax.getId();
        this.gridOperator = tax.getGridOperator();
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
    }
}
