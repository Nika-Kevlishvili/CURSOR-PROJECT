package bg.energo.phoenix.billingRun.model.entity;

import io.hypersistence.utils.hibernate.type.array.LongArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_period", schema = "billing_run")
@Data
public class BillingRunSettlementPeriod {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "contract_detail_id", nullable = false)
    private Long contractDetailId;

    @Column(name = "run_contract_id")
    private Long runContractId;

    @Column(name = "price_component_id")
    private Long priceComponentId;

    @Column(name = "pc_group_id")
    private Long pcGroupId;

    @Column(name = "bg_invoice_slot_id")
    private Long bgInvoiceSlotId;

    @Column(name = "bd_mapping_id")
    private Long bdMappingId;

    @Column(name = "price_formula")
    private String priceFormula;

    @Column(name = "run_total_price")
    private BigDecimal runTotalPrice;

    @Column(name = "billing_run_id")
    private Long billingRunId;

    @Column(name = "data_json")
    private String dataJson;

    @Column(name = "run_total_consumption")
    private BigDecimal runTotalConsumption;

    @Column(name = "measurement_type")
    private String measurementType;

    @Column(name = "scale_value_for_combined")
    private BigDecimal scaleValueForCombined;

    @Column(name = "percent")
    private BigDecimal percent;

    @Column(name = "date_from")
    private LocalDateTime dateFrom;

    @Column(name = "date_to")
    private LocalDateTime dateTo;

    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "pod_detail_id")
    private Long podDetailId;

    @Column(name = "profile_total_for_combined")
    private BigDecimal profileTotalForCombined;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Type(LongArrayType.class)
    @Column(name = "billing_data_profile_ids")
    private Long[]  billingDataProfileIds;

    @Type(LongArrayType.class)
    @Column(name = "billing_data_scale_ids")
    private Long[]  billingDataScaleIds;

    @Column(name = "price_component_price_type_id")
    private Long priceComponentPriceTypeId;
}

