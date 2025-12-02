package bg.energo.phoenix.model.entity.contract.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "contracts", schema = "product_contract")

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductContract extends BaseEntity {
    @Id
    @SequenceGenerator(name = "contracts_id_seq", sequenceName = "product_contract.contracts_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contracts_id_seq")
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductContractStatus status;

    @Column(name = "contract_number")
    private String contractNumber;

    @Column(name = "contract_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractDetailsStatus contractStatus;

    @Column(name = "contract_status_modify_date")
    private LocalDate statusModifyDate;

    @Column(name = "contract_sub_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractDetailsSubStatus subStatus;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "contract_term_end_date")
    private LocalDate contractTermEndDate;

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "perpetuity_date")
    private LocalDate perpetuityDate;

    @Column(name = "supply_activation_date")
    private LocalDate supplyActivationDate;

    @Column(name = "entry_into_force_date")
    private LocalDate entryIntoForceDate;

    @Column(name = "initial_term_start_date")
    private LocalDate initialTermDate;

    @Column(name = "signing_date")
    private LocalDate signingDate;

    @Column(name = "resign_to_contract_id")
    private Long resignedTo;

    @Column(name = "is_locked")
    private Boolean locked = Boolean.FALSE;

    public ProductContract(ProductContractStatus status) {
        this.status = status;
    }
}
