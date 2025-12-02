package bg.energo.phoenix.model.entity.nomenclature.receivable;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.SupplierType;
import bg.energo.phoenix.model.request.receivable.taxForTheGridOperator.TaxForTheGridOperatorRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Setter
@Getter
@Table(name = "grid_operator_taxes",schema = "nomenclature")
public class TaxesForTheGridOperator extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "grid_operator_taxes_id_seq",
            sequenceName = "nomenclature.grid_operator_taxes_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "grid_operator_taxes_id_seq")
    private Long id;

    @Column(name = "grid_operator_id")
    private Long gridOperator;

    @Column(name = "tax_for_reconnection")
    private BigDecimal taxForReconnection;

    @Column(name = "tax_for_express_reconnection")
    private BigDecimal taxForReconnectionExpress;

    @Column(name = "currency_id")
    private Long currency;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "disconnection_type")
    private String disconnectionType;

    @Column(name = "supplier_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SupplierType supplierType;

    @Column(name = "remove_tax_in_cancelation")
    private boolean removeTaxInCancel;

    @Column(name = "default_for_pod_with_measurement_type_slp")
    private boolean defaultForPodWithMeasurementTypeSlp;

    @Column(name = "default_for_pod_with_measurement_type_by_settlement_period")
    private boolean defaultForPodWithMeasurementTypeBySettlementPeriod;

    @Column(name = "document_template_id")
    private Long documentTemplateId;

    @Column(name = "email_template_id")
    private Long emailTemplateId;

    @Column(name = "number_of_income_account")
    private String numberOfIncomeAccount;

    @Column(name = "basis_for_issuing")
    private String basisForIssuing;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "price_component_or_price_component_group_or_item")
    private String priceComponentOrPriceComponentGroupOrItem;

    public TaxesForTheGridOperator(TaxForTheGridOperatorRequest request) {
        this.taxForReconnection = request.getTaxForReconnection();
        this.taxForReconnectionExpress = request.getTaxForExpressReconnection();
        this.removeTaxInCancel = request.isRemoveTaxInCancel();
        this.defaultForPodWithMeasurementTypeSlp = request.isDefaultForPodWithMeasurementTypeSlp();
        this.defaultForPodWithMeasurementTypeBySettlementPeriod = request.isDefaultForPodWithMeasurementTypeBySettlementPeriod();
        this.status = request.getStatus();
        this.supplierType = request.getSupplierType();
        this.documentTemplateId=request.getDocumentTemplateId();
        this.emailTemplateId=request.getEmailTemplateId();
        this.numberOfIncomeAccount=request.getNumberOfIncomeAccount();
        this.basisForIssuing=request.getBasisForIssuing();
        this.costCenterControllingOrder=request.getCostCenterControllingOrder();
        this.priceComponentOrPriceComponentGroupOrItem = request.getPriceComponentOrPriceComponentGroupOrItem();
    }
}
