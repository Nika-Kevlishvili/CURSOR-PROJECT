package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "service_contract", name = "contracts")
public class ServiceContracts extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "contracts_seq",
            sequenceName = "service_contract.contracts_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "contracts_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "contract_number")
    private String contractNumber;

    @Column(name = "contract_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ServiceContractDetailStatus contractStatus;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "signing_date")
    private LocalDate signingDate;

    @Column(name = "entry_into_force_date")
    private LocalDate entryIntoForceDate;

    @Column(name = "contract_term_end_date")
    private LocalDate contractTermEndDate;

    @Column(name = "perpetuity_date")
    private LocalDate perpetuityDate;

    @Column(name = "initial_term_start_date")
    private LocalDate contractInitialTermStartDate;

    //private StartOfContractInitialTerm startOfContractInitialTerm; //TODO should be added

    @Column(name = "contract_status_modify_date")
    private LocalDate statusModifyDate;

    //private StartOfContractInitialTerm startOfContractInitialTerm; //TODO need in database
    @Column(name = "is_locked")
    @Builder.Default
    private Boolean locked = Boolean.FALSE;

    @Column(name = "contract_sub_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ServiceContractDetailsSubStatus subStatus;
}
