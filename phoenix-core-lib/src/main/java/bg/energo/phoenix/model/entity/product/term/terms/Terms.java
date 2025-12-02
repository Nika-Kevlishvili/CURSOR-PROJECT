package bg.energo.phoenix.model.entity.product.term.terms;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.term.terms.*;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "terms", schema = "terms")
public class Terms extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "terms_id_seq",
            sequenceName = "terms.terms_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "terms_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "contract_delivery_activation_value")
    private Integer contractDeliveryActivationValue;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "contract_delivery_activation_type")
    private ContractDeliveryActivationType contractDeliveryActivationType;

    @Column(name = "contract_delivery_activation_auto_termination")
    private Boolean contractDeliveryActivationAutoTermination;

    @Column(name = "resigning_deadline_value")
    private Integer resigningDeadlineValue;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "resigning_deadline_type")
    private ResigningDeadlineType resigningDeadlineType;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "terms.terms_supply_activation"
            )
    )
    @Column(name = "supply_activation", columnDefinition = "terms.terms_supply_activation[]")
    private List<SupplyActivation> supplyActivations;

    @Column(name = "supply_activation_exact_date_start_day")
    private Integer supplyActivationExactDateStartDay;

    @Column(name = "general_notice_period_value")
    private Integer generalNoticePeriodValue;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "general_notice_period_type")
    private GeneralNoticePeriodType generalNoticePeriodType;

    @Column(name = "notice_term_period_value")
    private Integer noticeTermPeriodValue;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "notice_term_period_type")
    private NoticeTermPeriodType noticeTermPeriodType;

    @Column(name = "notice_term_disconnection_period_value")
    private Integer noticeTermDisconnectionPeriodValue;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "notice_term_disconnection_period_type")
    private NoticeTermDisconnectionPeriodType noticeTermDisconnectionPeriodType;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "terms.terms_contract_entry_into_force"
            )
    )
    @Column(name = "contract_entry_into_force", columnDefinition = "terms.terms_contract_entry_into_force[]")
    private List<ContractEntryIntoForce> contractEntryIntoForces;

    @Column(name = "contract_entry_into_force_exact_day_of_month_start_day")
    private Integer contractEntryIntoForceFromExactDayOfMonthStartDay;

    @Column(name = "no_interest_on_overdue_debts")
    private Boolean noInterestOnOverdueDebts;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private TermStatus status;

    @Column(name = "group_detail_id")
    private Long groupDetailId;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "terms.term_start_initial_term_of_contract"
            )
    )
    @Column(name = "start_initial_term_of_contract", columnDefinition = "terms.term_start_initial_term_of_contract[]")
    private List<StartOfContractInitialTerm> startsOfContractInitialTerms;

    @Column(name = "start_initial_term_of_contract_day")
    private Integer startDayOfInitialContractTerm;
    @Column(name = "first_day_of_month_initial_contract_term")
    private Integer firstDayOfTheMonthOfInitialContractTerm;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "terms.terms_wait_for_old_contract_term_to_expire"
            )
    )
    @Column(name = "wait_for_old_contract_term_to_expire", columnDefinition = "terms.terms_wait_for_old_contract_term_to_expire[]")
    private List<WaitForOldContractTermToExpire> waitForOldContractTermToExpires;

}
