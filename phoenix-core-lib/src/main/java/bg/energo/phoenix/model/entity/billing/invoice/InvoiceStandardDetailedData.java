package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.billingRun.model.BillingRunInvoiceDetailBaseModel;
import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;
import bg.energo.phoenix.model.enums.contract.ContractType;
import io.hypersistence.utils.hibernate.type.array.LongArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "invoice_standard_detailed_data", schema = "invoice")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceStandardDetailedData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "invoice_id", insertable = false, updatable = false)
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "detail_type", nullable = false)
    private InvoiceStandardDetailType detailType;

    @Column(name = "pc_id")
    private Long pcId;

    @Column(name = "pc_group_detail_id")
    private Long pcGroupDetailId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "pod_detail_id")
    private Long podDetailId;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "product_contract_detail_id")
    private Long productContractDetailId;

    @Column(name = "service_contract_detail_id")
    private Long serviceContractDetailId;

    @Column(name = "service_detail_id")
    private Long serviceDetailId;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Column(name = "total_volumes")
    private BigDecimal totalVolumes;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "main_currency_total_amount_without_vat")
    private BigDecimal mainCurrencyTotalAmountWithoutVat;

    @Column(name = "main_currency_total_amount_with_vat")
    private BigDecimal mainCurrencyTotalAmountWithVat;

    @Column(name = "main_currency_total_amount_vat")
    private BigDecimal mainCurrencyTotalAmountVat;

    @Column(name = "main_currency_id")
    private Long mainCurrencyId;

    @Column(name = "alt_currency_total_amount_without_vat")
    private BigDecimal altCurrencyTotalAmountWithoutVat;

    @Column(name = "alt_currency_total_amount_with_vat")
    private BigDecimal altCurrencyTotalAmountWithVat;

    @Column(name = "alt_currency_total_amount_vat")
    private BigDecimal altCurrencyTotalAmountVat;

    @Column(name = "alt_currency_id")
    private Long altCurrencyId;

    @Column(name = "original_currency_total_amount_without_vat")
    private BigDecimal originalCurrencyTotalAmountWithoutVat;

    @Column(name = "original_currency_total_amount_with_vat")
    private BigDecimal originalCurrencyTotalAmountWithVat;

    @Column(name = "original_currency_total_amount_vat")
    private BigDecimal originalCurrencyTotalAmountVat;

    @Column(name = "original_currency_id")
    private Long originalCurrencyId;

    @Column(name = "vat_rate_id")
    private Long vatRateId;

    @Column(name = "vat_rate_percent")
    private BigDecimal vatRatePercent;

    @Column(name = "meter_id")
    private Long meterId;

    @Column(name = "new_meter_reading")
    private BigDecimal newMeterReading;

    @Column(name = "old_meter_reading")
    private BigDecimal oldMeterReading;

    @Column(name = "difference")
    private BigDecimal difference;

    @Column(name = "multiplier")
    private BigDecimal multiplier;

    @Column(name = "correction")
    private BigDecimal correction;

    @Column(name = "deducted")
    private BigDecimal deducted;

    @Column(name = "measures_unit_for_total_volumes")
    private Integer measuresUnitForTotalVolumes;

    @Column(name = "measure_unit_for_unit_price")
    private Integer measureUnitForUnitPrice;

    @Column(name = "income_account_number")
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "interim_deduction_invoice_id")
    private Long interimDeductionInvoiceId;

    @Column(name = "tariff")
    private Boolean tariff;

    @Column(name = "interim_id")
    private Long interimId;

    @Column(name = "discount_id")
    private Long discountId;

    @Type(LongArrayType.class)
    @Column(name = "billing_data_profile_ids")
    private Long[] billingDataProfileIds;

    @Type(LongArrayType.class)
    @Column(name = "billing_data_scale_ids")
    private Long[] billingDataScaleIds;

    @Column(name = "price_component_price_type_id")
    private Long priceComponentPriceTypeId;

    @Column(name = "restricted")
    private Boolean restricted;

    @Column(name = "discounted")
    private Boolean discounted;

    @Column(name = "unrecognized_pod")
    private String unrecognizedPod;

    @Column(name = "scale_id")
    private Integer scaleId;

    public InvoiceStandardDetailedData(InvoiceStandardDetailType detailType,
                                       Long invoiceId,
                                       Invoice invoice,
                                       Long customerDetailId,
                                       String costCenterControllingOrder,
                                       String incomeAccountNumber,
                                       Long mainCurrencyId,
                                       Long altCurrencyId,
                                       BigDecimal mainCurrencyTotalAmountWithoutVat,
                                       BigDecimal mainCurrencyTotalAmountWithVat,
                                       BigDecimal mainCurrencyTotalAmountVat,
                                       Long vatRateId,
                                       BigDecimal vatRatePercent,
                                       BigDecimal altCurrencyTotalAmountWithoutVat,
                                       BigDecimal altCurrencyTotalAmountWithVat,
                                       BigDecimal altCurrencyTotalAmountVat) {
        this.detailType = detailType;
        this.invoiceId = invoiceId;
        this.invoice = invoice;
        this.customerDetailId = customerDetailId;
        this.costCenterControllingOrder = costCenterControllingOrder;
        this.incomeAccountNumber = incomeAccountNumber;
        this.mainCurrencyId = mainCurrencyId;
        this.altCurrencyId = altCurrencyId;
        this.mainCurrencyTotalAmountWithoutVat = mainCurrencyTotalAmountWithoutVat;
        this.mainCurrencyTotalAmountWithVat = mainCurrencyTotalAmountWithVat;
        this.mainCurrencyTotalAmountVat = mainCurrencyTotalAmountVat;
        this.vatRateId = vatRateId;
        this.vatRatePercent = vatRatePercent;
        this.altCurrencyTotalAmountWithoutVat = altCurrencyTotalAmountWithoutVat;
        this.altCurrencyTotalAmountWithVat = altCurrencyTotalAmountWithVat;
        this.altCurrencyTotalAmountVat = altCurrencyTotalAmountVat;
    }

    public InvoiceStandardDetailedData(BillingRunInvoiceDetailBaseModel model, Invoice invoice, ContractType contractType) {
        this.invoice = invoice;
        this.detailType = model.getType();
        this.pcId = model.getPcId();
        this.podId = model.getPodId();
        this.podDetailId = model.getPodDetailId();
        this.dateFrom = model.getDateFrom() != null ? model.getDateFrom() : null;
        this.dateTo = model.getDateTo() != null ? model.getDateTo().toLocalDate() : null;
        this.customerDetailId = model.getCustomerDetailId();
        this.productContractDetailId = contractType.equals(ContractType.PRODUCT_CONTRACT) ? model.getContractDetailId() : null;
        this.serviceContractDetailId = contractType.equals(ContractType.SERVICE_CONTRACT) ? model.getContractDetailId() : null;
        this.serviceDetailId = model.getServiceDetailId();
        this.productDetailId = model.getProductDetailId();
        this.totalVolumes = model.getInvoiceTotalConsumption();
        this.unitPrice = Objects.equals(model.getType(), InvoiceStandardDetailType.DISCOUNT) ? model.getDiscountPerKWH() : model.getKwhPrice();

        this.mainCurrencyTotalAmountWithoutVat = model.getInvoiceMainCurrencyAmountWithoutVat();
        this.mainCurrencyTotalAmountWithVat = model.getInvoiceMainCurrencyAmountWithVat();
        this.mainCurrencyTotalAmountVat = model.getInvoiceMainCurrencyVatAmount();
        this.mainCurrencyId = model.getInvoiceMainCurrencyId();

        this.altCurrencyTotalAmountWithoutVat = model.getInvoiceAltCurrencyAmountWithoutVat();
        this.altCurrencyTotalAmountWithVat = model.getInvoiceAltCurrencyAmountWithVat();
        this.altCurrencyTotalAmountVat = model.getInvoiceAltCurrencyVatAmount();
        this.altCurrencyId = model.getInvoiceAltCurrencyId();

        this.originalCurrencyTotalAmountWithoutVat = model.getInvoiceOriginalCurrencyAmountWithoutVat();
        this.originalCurrencyTotalAmountWithVat = model.getInvoiceOriginalCurrencyAmountWithVat();
        this.originalCurrencyTotalAmountVat = model.getInvoiceOriginalCurrencyVatAmount();
        this.originalCurrencyId = model.getInvoiceOriginalCurrencyId();

        this.vatRateId = model.getInvoiceVatId();
        this.vatRatePercent = model.getInvoiceVatPercent();

        this.meterId = model.getMeterId();
        this.newMeterReading = model.getNewMeterReading();
        this.oldMeterReading = model.getOldMeterReading();
        this.difference = model.getDifference();
        this.multiplier = model.getMultiplier();
        this.correction = model.getCorrection();
        this.measuresUnitForTotalVolumes = model.getMeasuresUnitForTotalVolumes();
        this.measureUnitForUnitPrice = model.getMeasureUnitForUnitPrice();
        this.incomeAccountNumber = model.getIncomeAccountNumber();
        this.costCenterControllingOrder = model.getCostCenterControllingOrder();
        this.deducted = model.getDeducted();
        this.pcGroupDetailId = model.getPcGroupId();
        this.tariff = model.isTariff();
        this.discountId = model.getDiscountId();
        this.billingDataProfileIds = model.getBillingDataProfileIds();
        this.billingDataScaleIds = model.getBillingDataScaleIds();
        this.priceComponentPriceTypeId = model.getPriceComponentPriceTypeId();
        this.discounted = model.getDiscounted();
        this.restricted = model.getRestricted();
        this.unrecognizedPod = model.getUnrecognizedPod();
        this.scaleId = model.getScaleId();
    }

    public InvoiceStandardDetailedData cloneForReversal(Invoice newInvoice) {
        InvoiceStandardDetailedData cloned = new InvoiceStandardDetailedData();
        cloned.setInvoice(newInvoice);
        cloned.setDetailType(this.detailType);
        cloned.setPcId(this.pcId);
        cloned.setPcGroupDetailId(this.pcGroupDetailId);
        cloned.setPodId(this.podId);
        cloned.setDateFrom(this.dateFrom);
        cloned.setDateTo(this.dateTo);
        cloned.setCustomerDetailId(this.customerDetailId);
        cloned.setProductContractDetailId(this.productContractDetailId);
        cloned.setServiceContractDetailId(this.serviceContractDetailId);
        cloned.setServiceDetailId(this.serviceDetailId);
        cloned.setProductDetailId(this.productDetailId);
        cloned.setTotalVolumes(this.totalVolumes);
        cloned.setUnitPrice(this.unitPrice);
        cloned.setMainCurrencyTotalAmountWithoutVat(this.mainCurrencyTotalAmountWithoutVat);
        cloned.setMainCurrencyTotalAmountWithVat(this.mainCurrencyTotalAmountWithVat);
        cloned.setMainCurrencyTotalAmountVat(this.mainCurrencyTotalAmountVat);
        cloned.setMainCurrencyId(this.mainCurrencyId);
        cloned.setAltCurrencyTotalAmountWithoutVat(this.altCurrencyTotalAmountWithoutVat);
        cloned.setAltCurrencyTotalAmountWithVat(this.altCurrencyTotalAmountWithVat);
        cloned.setAltCurrencyTotalAmountVat(this.altCurrencyTotalAmountVat);
        cloned.setAltCurrencyId(this.altCurrencyId);
        cloned.setOriginalCurrencyTotalAmountWithoutVat(this.originalCurrencyTotalAmountWithoutVat);
        cloned.setOriginalCurrencyTotalAmountWithVat(this.originalCurrencyTotalAmountWithVat);
        cloned.setOriginalCurrencyTotalAmountVat(this.originalCurrencyTotalAmountVat);
        cloned.setOriginalCurrencyId(this.originalCurrencyId);
        cloned.setVatRateId(this.vatRateId);
        cloned.setVatRatePercent(this.vatRatePercent);
        cloned.setMeterId(this.meterId);
        cloned.setNewMeterReading(this.newMeterReading);
        cloned.setOldMeterReading(this.oldMeterReading);
        cloned.setDifference(this.difference);
        cloned.setMultiplier(this.multiplier);
        cloned.setCorrection(this.correction);
        cloned.setDeducted(this.deducted);
        cloned.setMeasuresUnitForTotalVolumes(this.measuresUnitForTotalVolumes);
        cloned.setMeasureUnitForUnitPrice(this.measureUnitForUnitPrice);
        cloned.setIncomeAccountNumber(this.incomeAccountNumber);
        cloned.setCostCenterControllingOrder(this.costCenterControllingOrder);
        cloned.setInterimDeductionInvoiceId(this.interimDeductionInvoiceId);
        cloned.setTariff(this.tariff);
        cloned.setInterimId(this.interimId);
        cloned.setDiscountId(this.discountId);
        cloned.setBillingDataProfileIds(this.billingDataProfileIds);
        cloned.setBillingDataScaleIds(this.billingDataScaleIds);
        cloned.setPriceComponentPriceTypeId(this.priceComponentPriceTypeId);
        cloned.setRestricted(this.restricted);
        cloned.setDiscounted(this.discounted);
        cloned.setUnrecognizedPod(this.unrecognizedPod);
        cloned.setScaleId(this.scaleId);
        return cloned;
    }
}
