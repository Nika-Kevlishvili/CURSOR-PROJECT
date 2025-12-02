package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.service.ContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractContractType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "service_contract", name = "contract_details")
public class ServiceContractDetails extends BaseEntity {
    @Id
    @SequenceGenerator(name = "contract_details_seq", sequenceName = "service_contract.contract_details_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_details_seq")
    private Long id;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ServiceContractContractType type;

    /*@Column(name = "activation_date")
    private LocalDate activationDate;*/

    @Column(name = "contract_term_until_the_amount")
    private Boolean contractTermUntilTheAmount;

    @Column(name = "contract_term_until_the_amount_value")
    private BigDecimal contractTermUntilTheAmountValue;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "cash_deposit_currency_id")
    private Long cashDepositCurrency;

    @Column(name = "service_detail_id")
    private Long serviceDetailId;

    @Column(name = "customer_communication_id_for_billing")
    private Long customerCommunicationIdForBilling;

    @Column(name = "customer_communication_id_for_contract")
    private Long customerCommunicationIdForContract;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "direct_debit")
    private Boolean directDebit;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "iban")
    private String iban;

    @Column(name = "applicable_interest_rate")
    private Long applicableInterestRate;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "service_contract_term_id")
    private Long serviceContractTermId;

    @Column(name = "invoice_payment_term_id")
    private Long invoicePaymentTermId;

    @Column(name = "payment_guarantee")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PaymentGuarantee paymentGuarantee;

    @Column(name = "invoice_payment_term_value")
    private Integer invoicePaymentTermValue;

    @Column(name = "cash_deposit_amount")
    private BigDecimal cashDepositAmount;

    @Column(name = "equal_monthly_installment_number")
    private Short equalMonthlyInstallmentNumber;

    @Column(name = "equal_monthly_installment_amount")
    private BigDecimal equalMonthlyInstallmentAmount;

    @Column(name = "bank_guarantee_amount")
    private BigDecimal bankGuaranteeAmount;

    @Column(name = "bank_guarantee_currency_id")
    private Long bankGuaranteeCurrencyId;

    @Column(name = "guarantee_contract")
    private Boolean guaranteeContract;

    @Column(name = "guarantee_contract_info")
    private String guaranteeContractInfo;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractVersionStatus contractVersionStatus;

    @Column(name = "entry_into_force")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractEntryIntoForce entryIntoForce;

    @Column(name = "entry_into_force_value")
    private LocalDate entryIntoForceValue;

    @Column(name = "initial_term_start_value")
    private LocalDate initialTermStartValue;

    @Column(name = "contract_term_end_date")
    private LocalDate contractTermEndDate;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "additional_agreement_suffix")
    private Integer agreementSuffix;

    @Column(name = "start_initial_term")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StartOfContractInitialTerm startOfContractInitialTerm;

}
