package bg.energo.phoenix.model.entity.product.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermPeriodType;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermRenewalType;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermType;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor

@Table(schema = "service", name = "service_contract_terms")
public class ServiceContractTerm extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "service_contract_terms_id_seq",
            sequenceName = "service.service_contract_terms_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_contract_terms_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "contract_term_period_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private ServiceContractTermPeriodType periodType;

    @Column(name = "contract_term_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private ServiceContractTermType termType;

    @Column(name = "value")
    private Integer value;

    @Column(name = "perpetuity_clause")
    private Boolean perpetuityClause;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_details_id", referencedColumnName = "id")
    private ServiceDetails serviceDetails;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ServiceSubobjectStatus status;

    @Column(name = "automatic_renewal")
    private Boolean automaticRenewal;

    @Column(name = "number_of_renewals")
    private Integer numberOfRenewals;

    @Column(name = "renewal_period_value")
    private Integer renewalPeriodValue;

    @Column(name = "renewal_period_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private ServiceContractTermRenewalType renewalPeriodType;

}
