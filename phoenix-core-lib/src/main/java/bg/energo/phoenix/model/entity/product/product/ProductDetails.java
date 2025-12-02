package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.ElectricityPriceType;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductGroups;
import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductTypes;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.enums.product.product.*;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "product", name = "product_details")
public class ProductDetails extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "product_details_id_seq", sequenceName = "product.product_details_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_details_id_seq")
    private Long id;

    @Column(name = "version_id")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Product product;

    @OneToOne
    @JoinColumn(name = "product_balancing_id_for_consumer", referencedColumnName = "id")
    private ProductForBalancing productBalancingIdForConsumer;

    @OneToOne
    @JoinColumn(name = "product_balancing_id_for_generator", referencedColumnName = "id")
    private ProductForBalancing productBalancingIdForGenerator;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductDetailStatus productDetailStatus;

    @Nationalized
    @Column(name = "name", length = 1024, nullable = false)
    private String name;

    @Column(name = "name_transl", length = 1024, nullable = false)
    private String nameTransliterated;

    @Column(name = "available_for_sale", nullable = false)
    private Boolean availableForSale;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_to")
    private LocalDateTime availableTo;

    @Nationalized
    @Column(name = "printing_name", nullable = false, length = 1024)
    private String printingName;

    @Column(name = "printing_name_transl", nullable = false, length = 1024)
    private String printingNameTransliterated;

    @Column(name = "short_description", nullable = false, length = 8192)
    private String shortDescription;

    @Column(name = "full_description", nullable = false, length = 32768)
    private String fullDescription;

    @Column(name = "invoice_and_templates_text", length = 2048, nullable = false)
    private String invoiceAndTemplatesText;

    @Column(name = "invoice_and_templates_text_transl", length = 2048, nullable = false)
    private String invoiceAndTemplatesTextTransliterated;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_group_id", referencedColumnName = "id")
    private ProductGroups productGroups;

    @Column(name = "other_system_connection_code", length = 256)
    private String otherSystemConnectionCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", referencedColumnName = "id", nullable = false)
    private ProductTypes productType;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.product_contract_type"
            )
    )
    @Column(name = "contract_type", columnDefinition = "product.product_contract_type[]")
    private List<ContractType> contractTypes;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.product_payment_guarantee"
            )
    )
    @Column(name = "payment_guarantee", columnDefinition = "product.product_payment_guarantee[]")
    private List<PaymentGuarantee> paymentGuarantees;

    @Column(name = "cash_deposit_amount")
    private BigDecimal cashDepositAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_deposit_currency_id", referencedColumnName = "id")
    private Currency cashDepositCurrency;

    @Column(name = "bank_guarantee_amount")
    private BigDecimal bankGuaranteeAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_guarantee_currency_id", referencedColumnName = "id")
    private Currency bankGuaranteeCurrency;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.product_consumption_purpose"
            )
    )
    @Column(name = "consumption_purpose", columnDefinition = "product.product_consumption_purpose[]")
    private List<PurposeOfConsumption> consumptionPurposes;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.product_pod_metering_type"
            )
    )
    @Column(name = "pod_metering_type", columnDefinition = "product.product_pod_metering_type[]")
    private List<MeteringTypeOfThePointOfDelivery> meteringTypeOfThePointOfDeliveries;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.product_voltage_level"
            )
    )
    @Column(name = "voltage_level", columnDefinition = "product.product_voltage_level[]")
    private List<VoltageLevel> voltageLevels;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.product_pod_type"
            )
    )
    @Column(name = "pod_type", columnDefinition = "product.product_pod_type[]")
    private List<ProductPodType> productPodTypes;

    @Column(name = "income_account_number", length = 32)
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order", length = 32)
    private String costCenterControllingOrder;

    @Column(name = "global_vat_rate")
    private Boolean globalVatRate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_rate_id", referencedColumnName = "id")
    private VatRate vatRate;

    @Column(name = "global_sales_area")
    private Boolean globalSalesArea;

    @Column(name = "global_sales_channel")
    private Boolean globalSalesChannel;

    @Column(name = "global_segment")
    private Boolean globalSegment;

    @Column(name = "global_grid_operator")
    private Boolean globalGridOperators;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "electricity_price_type_id", referencedColumnName = "id")
    private ElectricityPriceType electricityPriceType;

    @Column(name = "equal_monthly_installments_activation")
    private Boolean equalMonthlyInstallmentsActivation;

    @Column(name = "installment_number")
    private Short installmentNumber;

    @Column(name = "installment_number_from")
    private Short installmentNumberFrom;

    @Column(name = "installment_number_to")
    private Short installmentNumberTo;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "amount_from")
    private BigDecimal amountFrom;

    @Column(name = "amount_to")
    private BigDecimal amountTo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", referencedColumnName = "id")
    private Currency currency;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.product_schedule_registration"
            )
    )
    @Column(name = "schedule_registration", columnDefinition = "product.product_schedule_registration[]")
    private List<ScheduleRegistrations> scheduleRegistrations;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.product_forecasting"
            )
    )
    @Column(name = "forecasting", columnDefinition = "product.product_forecasting[]")
    private List<Forecasting> forecasting;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.product_taking_over_balancing_cost"
            )
    )
    @Column(name = "taking_over_balancing_cost", columnDefinition = "product.product_taking_over_balancing_cost[]")
    private List<TakingOverBalancingCosts> takingOverBalancingCosts;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "provided_capacity_limit_type")
    private ProvidedCapacityLimitType capacityLimitType;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.payment_channels"
            )
    )
    @Column(name = "ineligible_payment_channel", columnDefinition = "product.payment_channels[]")
    private List<PaymentChannels> paymentChannels;

    @Column(name = "provided_capacity_limit_amount")
    private Short capacityLimitAmount;

    @Column(name = "additional_info1")
    private String additionalInfo1;

    @Column(name = "additional_info2")
    private String additionalInfo2;

    @Column(name = "additional_info3")
    private String additionalInfo3;

    @Column(name = "additional_info4")
    private String additionalInfo4;

    @Column(name = "additional_info5")
    private String additionalInfo5;

    @Column(name = "additional_info6")
    private String additionalInfo6;

    @Column(name = "additional_info7")
    private String additionalInfo7;

    @Column(name = "additional_info8")
    private String additionalInfo8;

    @Column(name = "additional_info9")
    private String additionalInfo9;

    @Column(name = "additional_info10")
    private String additionalInfo10;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", referencedColumnName = "id")
    private Terms terms;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_group_id", referencedColumnName = "id")
    private TermsGroups termsGroups;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductGridOperator> gridOperator;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductSalesChannel> salesChannels;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductSalesArea> salesAreas;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductSegments> segments;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductTerminations> terminations;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductTerminationGroups> terminationGroups;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductPenalty> penalties;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductPenaltyGroups> penaltyGroups;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductPriceComponents> priceComponents;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductPriceComponentGroups> priceComponentGroups;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductInterimAndAdvancePayments> interimAndAdvancePayments;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductGroupOfInterimAndAdvancePayments> interimAndAdvancePaymentGroups;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductLinkToProduct> linkedProducts;

    @OneToMany(mappedBy = "productDetails", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductLinkToService> linkedServices;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "product_detail_id", referencedColumnName = "id")
    private List<ProductFile> productFiles;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "product_detail_id", referencedColumnName = "id")
    private List<ProductAdditionalParams> productAdditionalParams;

    @Override
    public String toString() {
        return "ProductDetails{" +
               "id=" + id +
               ", version=" + version +
               ", productDetailStatus=" + productDetailStatus +
               ", name='" + name + '\'' +
               ", nameTransliterated='" + nameTransliterated + '\'' +
               ", availableForSale=" + availableForSale +
               ", availableFrom=" + availableFrom +
               ", availableTo=" + availableTo +
               ", printingName='" + printingName + '\'' +
               ", printingNameTransliterated='" + printingNameTransliterated + '\'' +
               ", shortDescription='" + shortDescription + '\'' +
               ", fullDescription='" + fullDescription + '\'' +
               ", invoiceAndTemplatesText='" + invoiceAndTemplatesText + '\'' +
               ", invoiceAndTemplatesTextTransliterated='" + invoiceAndTemplatesTextTransliterated + '\'' +
               ", productGroups=" + productGroups +
               ", otherSystemConnectionCode='" + otherSystemConnectionCode + '\'' +
               ", productType=" + productType +
               ", contractTypes=" + contractTypes +
               ", paymentGuarantees=" + paymentGuarantees +
               ", cashDepositAmount=" + cashDepositAmount +
               ", cashDepositCurrency=" + cashDepositCurrency +
               ", bankGuaranteeAmount=" + bankGuaranteeAmount +
               ", bankGuaranteeCurrency=" + bankGuaranteeCurrency +
               ", consumptionPurposes=" + consumptionPurposes +
               ", meteringTypeOfThePointOfDeliveries=" + meteringTypeOfThePointOfDeliveries +
               ", voltageLevels=" + voltageLevels +
               ", productPodTypes=" + productPodTypes +
               ", incomeAccountNumber='" + incomeAccountNumber + '\'' +
               ", costCenterControllingOrder='" + costCenterControllingOrder + '\'' +
               ", globalVatRate=" + globalVatRate +
               ", vatRate=" + vatRate +
               ", globalSalesArea=" + globalSalesArea +
               ", globalSalesChannel=" + globalSalesChannel +
               ", globalSegment=" + globalSegment +
               ", electricityPriceType=" + electricityPriceType +
               ", equalMonthlyInstallmentsActivation=" + equalMonthlyInstallmentsActivation +
               ", installmentNumber=" + installmentNumber +
               ", installmentNumberFrom=" + installmentNumberFrom +
               ", installmentNumberTo=" + installmentNumberTo +
               ", amount=" + amount +
               ", amountFrom=" + amountFrom +
               ", amountTo=" + amountTo +
               ", currency=" + currency +
               ", scheduleRegistrations=" + scheduleRegistrations +
               ", forecasting=" + forecasting +
               ", takingOverBalancingCosts=" + takingOverBalancingCosts +
               ", capacityLimitType=" + capacityLimitType +
               ", paymentChannels=" + paymentChannels +
               ", capacityLimitAmount=" + capacityLimitAmount +
               ", additionalInfo1='" + additionalInfo1 + '\'' +
               ", additionalInfo2='" + additionalInfo2 + '\'' +
               ", additionalInfo3='" + additionalInfo3 + '\'' +
               ", additionalInfo4='" + additionalInfo4 + '\'' +
               ", additionalInfo5='" + additionalInfo5 + '\'' +
               ", additionalInfo6='" + additionalInfo6 + '\'' +
               ", additionalInfo7='" + additionalInfo7 + '\'' +
               ", additionalInfo8='" + additionalInfo8 + '\'' +
               ", additionalInfo9='" + additionalInfo9 + '\'' +
               ", additionalInfo10='" + additionalInfo10 + '\'' +
               ", terms=" + terms +
               ", termsGroups=" + termsGroups +
               '}';
    }
}
