package bg.energo.phoenix.model.entity.contract.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailType;
import bg.energo.phoenix.model.enums.product.product.ContractType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.model.enums.product.term.terms.WaitForOldContractTermToExpire;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contract_details", schema = "product_contract")

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductContractDetails extends BaseEntity {

    @Id
    @SequenceGenerator(name = "contract_details_id_seq", sequenceName = "product_contract.contract_details_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_details_id_seq")
    private Long id;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractDetailType type;

    @Column(name = "contract_term_until_the_amount")
    private Boolean contractTermUntilTheAmount;

    @Column(name = "contract_term_until_the_amount_value")
    private BigDecimal contractTermUntilTheAmountValue;

    @Column(name = "contract_term_until_the_volume")
    private Boolean contractTermUntilTheVolume;

    @Column(name = "contract_term_until_the_volume_value")
    private BigDecimal contractTermUntilTheVolumeValue;

    @Column(name = "public_procurement_law")
    private Boolean publicProcurementLaw;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Column(name = "customer_communication_id_for_billing")
    private Long customerCommunicationIdForBilling;

    @Column(name = "customer_communication_id_for_contract")
    private Long customerCommunicationIdForContract;

    @Column(name = "version_id")
    private Integer versionId;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "deal_number")
    private String dealNumber;

    @Column(name = "estimated_total_consumption_under_contract_kwh")
    private BigDecimal estimatedTotalConsumptionUnderContractKwh;

    @Column(name = "direct_debit")
    private Boolean directDebit;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "iban")
    private String iban;

    @Column(name = "risk_assessment")
    private String riskAssessment;

    @Column(name = "risk_assessment_additional_condition")
    private String riskAssessmentAdditionalCondition;

    @Column(name = "applicable_interest_rate")
    private Long applicableInterestRate;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "contract_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractType contractType;

    @Column(name = "product_contract_term_id")
    private Long productContractTermId;

    @Column(name = "invoice_payment_term_id")
    private Long invoicePaymentTermId;

    @Column(name = "payment_guarantee")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PaymentGuarantee paymentGuarantee;

    @Column(name = "invoice_payment_term_value")
    private Integer invoicePaymentTermValue;

    @Column(name = "entry_into_force")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractEntryIntoForce entryIntoForce;

    @Column(name = "supply_activation_after_contract_resigning")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SupplyActivation supplyActivationAfterContractResigning;

    @Column(name = "start_initial_term")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StartOfContractInitialTerm startInitialTerm;

    @Column(name = "equal_monthly_installment_number")
    private Short equalMonthlyInstallmentNumber;

    @Column(name = "equal_monthly_installment_amount")
    private BigDecimal equalMonthlyInstallmentAmount;

    @Column(name = "cash_deposit_currency_id")
    private Long cashDepositCurrencyId;

    @Column(name = "cash_deposit_amount")
    private BigDecimal cashDepositAmount;

    @Column(name = "bank_guarantee_currency_id")
    private Long bankGuaranteeCurrencyId;

    @Column(name = "bank_guarantee_amount")
    private BigDecimal bankGuaranteeAmount;

    @Column(name = "guarantee_contract_info")
    private String guaranteeContractInfo;

    @Column(name = "guarantee_contract")
    private Boolean guaranteeContract;

    @Column(name = "marginal_price")
    private BigDecimal marginalPrice;

    @Column(name = "marginal_price_validity")
    private String marginalPriceValidity;

    @Column(name = "avg_hourly_load_profiles")
    private BigDecimal avgHourlyLoadProfiles;

    @Column(name = "procurement_price")
    private BigDecimal procurementPrice;

    @Column(name = "cost_price_increase_from_imbalances")
    private BigDecimal costPriceIncreaseFromImbalances;

    @Column(name = "set_margin")
    private BigDecimal setMargin;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductContractVersionStatus versionStatus;

    @Column(name = "supply_activation_value")
    private LocalDate supplyActivationDate;
    @Column(name = "entry_into_force_value")
    private LocalDate entryIntoForceDate;
    @Column(name = "initial_term_start_value")
    private LocalDate initialTermDate;
    @Column(name = "contract_term_end_date")
    private LocalDate contractTermEndDate;

    @Column(name = "employee_id")
    private Long employeeId;
    @Column(name = "additional_agreement_suffix")
    private Integer agreementSuffix;
    @Column(name = "wait_for_old_contract_term_to_expire")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private WaitForOldContractTermToExpire waitContractExpire;

}
