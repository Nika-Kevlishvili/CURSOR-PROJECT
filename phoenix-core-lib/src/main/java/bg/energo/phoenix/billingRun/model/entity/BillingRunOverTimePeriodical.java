package bg.energo.phoenix.billingRun.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "over_time_periodical", schema = "billing_run")
@Data
public class BillingRunOverTimePeriodical {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "contract_detail_id", nullable = false)
    private Long contractDetailId;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Column(name = "run_contract_id")
    private Long runContractId;

    @Column(name = "price_component_id")
    private Long priceComponentId;

    @Column(name = "pc_group_id")
    private Long pcGroupId;

    @Column(name = "bg_invoice_slot_id")
    private Long bgInvoiceSlotId;

    @Column(name = "price_formula")
    private String priceFormula;

    @Column(name = "run_total_price")
    private BigDecimal runTotalPrice;

    @Column(name = "status")
    private String status;

    @Column(name = "billing_run_id")
    private Long billingRunId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "price_component_price_type_id")
    private Long priceComponentPriceTypeId;

    @Column(name = "unrecognized_pod")
    private String unrecognizedPod;
}

