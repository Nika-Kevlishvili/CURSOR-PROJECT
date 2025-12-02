package bg.energo.phoenix.model.entity.product.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceGroups;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceType;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceUnit;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.service.*;
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
@Table(schema = "service", name = "service_details")
public class ServiceDetails extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "service_details_id_seq",
            sequenceName = "service.service_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_details_id_seq"
    )
    private Long id;

    @Nationalized
    @Column(name = "name", length = 1024, nullable = false)
    private String name;

    @Column(name = "name_transl", length = 1024, nullable = false)
    private String nameTransliterated;

    @Nationalized
    @Column(name = "printing_name", nullable = false, length = 1024)
    private String printingName;

    @Column(name = "printing_name_transl", nullable = false, length = 1024)
    private String printingNameTransliterated;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_group_id", referencedColumnName = "id")
    private ServiceGroups serviceGroup;

    @Column(name = "other_system_connection_code", length = 256)
    private String otherSystemConnectionCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_type_id", referencedColumnName = "id", nullable = false)
    private ServiceType serviceType;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "service.service_sale_method"
            )
    )
    @Column(name = "sale_method", columnDefinition = "service.service_sale_method[]")
    private List<ServiceSaleMethod> saleMethods;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "service.service_payment_guarantee"
            )
    )
    @Column(name = "payment_guarantee", columnDefinition = "service.service_payment_guarantee[]")
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
                    value = "service.service_consumption_purpose"
            )
    )
    @Column(name = "consumption_purpose", columnDefinition = "service.service_consumption_purpose[]")
    private List<ServiceConsumptionPurpose> consumptionPurposes;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "service.service_pod_metering_type"
            )
    )
    @Column(name = "pod_metering_type", columnDefinition = "service.service_pod_metering_type[]")
    // POD => point of delivery
    private List<ServicePODMeteringType> podMeteringTypes;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "service.service_voltage_level"
            )
    )
    @Column(name = "voltage_level", columnDefinition = "service.service_voltage_level[]")
    private List<ServiceVoltageLevel> voltageLevels;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "service.service_pod_type"
            )
    )
    @Column(name = "pod_type", columnDefinition = "service.service_pod_type[]")
    private List<ServicePodType> podTypes;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ServiceDetailStatus status;

    @Column(name = "available_for_sale", nullable = false)
    private Boolean availableForSale;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_to")
    private LocalDateTime availableTo;

    @Column(name = "short_description", nullable = false, length = 8192)
    private String shortDescription;

    @Column(name = "full_description", length = 32768)
    private String fullDescription;

    @Column(name = "invoice_and_templates_text", length = 2048, nullable = false)
    private String invoiceAndTemplatesText;

    @Column(name = "invoice_and_templates_text_transl", length = 2048, nullable = false)
    private String invoiceAndTemplatesTextTransliterated;

    @Column(name = "income_account_number", length = 32)
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order", length = 32)
    private String costCenterControllingOrder;

    @Column(name = "global_vat_rate")
    private Boolean globalVatRate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_rate_id", referencedColumnName = "id")
    private VatRate vatRate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_unit_id", referencedColumnName = "id")
    private ServiceUnit serviceUnit;

    @Column(name = "global_sales_channel")
    private Boolean globalSalesChannel;

    @Column(name = "global_sales_area")
    private Boolean globalSalesArea;

    @Column(name = "global_segment")
    private Boolean globalSegment;

    @Column(name = "global_grid_operator")
    private Boolean globalGridOperator;

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

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "periodicity")
    private ServicePeriodicity servicePeriodicity;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private ServicePaymentMethod paymentMethod;

    @Column(name = "payment_before_execution")
    private Boolean paymentBeforeExecution;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_level")
    private ServiceExecutionLevel executionLevel;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "provided_capacity_limit_type")
    private ServiceProvidedCapacityLimitType capacityLimitType;

    @Column(name = "provided_capacity_limit_amount")
    private Short capacityLimitAmount;

    // TODO: 5/2/23 later this field will be replaced by a reference to Payment Channel object and globalIneligiblePaymentChannel field will be added too
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "service.service_ineligible_payment_channels"
            )
    )
    @Column(name = "ineligible_payment_channel", columnDefinition = "service.service_ineligible_payment_channels[]")
    private List<ServiceIneligiblePaymentChannel> ineligiblePaymentChannels;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", referencedColumnName = "id")
    private Terms terms;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_group_id", referencedColumnName = "id")
    private TermsGroups termsGroups;

    @Column(name = "version_id")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    private EPService service;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServiceGridOperator> gridOperator;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServiceSalesChannel> salesChannels;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServiceSalesArea> salesAreas;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServiceSegment> segments;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServicePenalty> penalties;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServicePenaltyGroup> penaltyGroups;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServicePriceComponent> priceComponents;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServicePriceComponentGroup> priceComponentGroups;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServiceTermination> terminations;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServiceTerminationGroup> terminationGroups;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServiceInterimAndAdvancePayment> interimAndAdvancePayments;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServiceInterimAndAdvancePaymentGroup> interimAndAdvancePaymentGroups;

    @OneToMany(mappedBy = "serviceDetails", cascade = CascadeType.MERGE)
    private List<ServiceContractTerm> contractTerms;

    @OneToMany(mappedBy = "serviceDetails", fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private List<ServiceLinkedService> linkedServices;

    @OneToMany(mappedBy = "serviceDetails", fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private List<ServiceLinkedProduct> linkedProducts;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "service_detail_id", referencedColumnName = "id")
    private List<ServiceFile> serviceFiles;

    @Column(name = "additional_info1", length = 1024)
    private String additionalField1;

    @Column(name = "additional_info2", length = 1024)
    private String additionalField2;

    @Column(name = "additional_info3", length = 1024)
    private String additionalField3;

    @Column(name = "additional_info4", length = 1024)
    private String additionalField4;

    @Column(name = "additional_info5", length = 1024)
    private String additionalField5;

    @Column(name = "additional_info6", length = 1024)
    private String additionalField6;

    @Column(name = "additional_info7", length = 1024)
    private String additionalField7;

    @Column(name = "additional_info8", length = 1024)
    private String additionalField8;

    @Column(name = "additional_info9", length = 1024)
    private String additionalField9;

    @Column(name = "additional_info10", length = 1024)
    private String additionalField10;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "service_detail_id", referencedColumnName = "id")
    private List<ServiceAdditionalParams> serviceAdditionalParams;

    @Override
    public String toString() {
        return "ServiceDetails{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", nameTransliterated='" + nameTransliterated + '\'' +
               ", printingName='" + printingName + '\'' +
               ", printingNameTransliterated='" + printingNameTransliterated + '\'' +
               ", serviceGroup=" + serviceGroup +
               ", otherSystemConnectionCode='" + otherSystemConnectionCode + '\'' +
               ", serviceType=" + serviceType +
               ", status=" + status +
               ", availableForSale=" + availableForSale +
               ", availableFrom=" + availableFrom +
               ", availableTo=" + availableTo +
               ", shortDescription='" + shortDescription + '\'' +
               ", fullDescription='" + fullDescription + '\'' +
               ", invoiceAndTemplatesText='" + invoiceAndTemplatesText + '\'' +
               ", invoiceAndTemplatesTextTransliterated='" + invoiceAndTemplatesTextTransliterated + '\'' +
               ", incomeAccountNumber='" + incomeAccountNumber + '\'' +
               ", costCenterControllingOrder='" + costCenterControllingOrder + '\'' +
               ", globalVatRate=" + globalVatRate +
               ", vatRate=" + vatRate +
               ", serviceUnit=" + serviceUnit +
               ", globalSalesChannel=" + globalSalesChannel +
               ", globalSalesArea=" + globalSalesArea +
               ", globalSegment=" + globalSegment +
               ", equalMonthlyInstallmentsActivation=" + equalMonthlyInstallmentsActivation +
               ", installmentNumber=" + installmentNumber +
               ", installmentNumberFrom=" + installmentNumberFrom +
               ", installmentNumberTo=" + installmentNumberTo +
               ", amount=" + amount +
               ", amountFrom=" + amountFrom +
               ", amountTo=" + amountTo +
               ", currency=" + currency +
               ", servicePeriodicity=" + servicePeriodicity +
               ", paymentMethod=" + paymentMethod +
               ", paymentBeforeExecution=" + paymentBeforeExecution +
               ", executionLevel=" + executionLevel +
               ", capacityLimitType=" + capacityLimitType +
               ", capacityLimitAmount=" + capacityLimitAmount +
               ", terms=" + terms +
               ", termsGroups=" + termsGroups +
               ", version=" + version +
               ", additionalField1='" + additionalField1 + '\'' +
               ", additionalField2='" + additionalField2 + '\'' +
               ", additionalField3='" + additionalField3 + '\'' +
               ", additionalField4='" + additionalField4 + '\'' +
               ", additionalField5='" + additionalField5 + '\'' +
               ", additionalField6='" + additionalField6 + '\'' +
               ", additionalField7='" + additionalField7 + '\'' +
               ", additionalField8='" + additionalField8 + '\'' +
               ", additionalField9='" + additionalField9 + '\'' +
               ", additionalField10='" + additionalField10 + '\'' +
               '}';
    }
}

