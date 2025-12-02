package bg.energo.phoenix.billingRun.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Entity
@Table(name = "over_time_with_electricity", schema = "billing_run")
public class BillingRunOverTimeWithElectricity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "run_contract_id")
    private Long runContractId;

    @Column(name = "price_component_id")
    private Long priceComponentId;

    @Column(name = "pc_group_id")
    private Long pcGroupId;

    @Column(name = "pod_count")
    private Integer podCount;

    @Column(name = "price_formula")
    private String priceFormula;

    @Column(name = "status")
    private String status;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "bg_invoice_slot_id")
    private Long bgInvoiceSlotId;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Column(name = "has_invalid_price_formula")
    private Boolean hasInvalidPriceFormula;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "price_component_price_type_id")
    private Long priceComponentPriceTypeId;
}
