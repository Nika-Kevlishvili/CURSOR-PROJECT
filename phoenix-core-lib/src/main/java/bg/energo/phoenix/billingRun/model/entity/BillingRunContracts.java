package bg.energo.phoenix.billingRun.model.entity;

import bg.energo.phoenix.model.enums.contract.ContractType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "run_contracts", schema = "billing_run")
@Data
public class BillingRunContracts {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "processing_status")
    private String processingStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "contract_type")
    private ContractType contractType;

    @Column(name = "error_message")
    private String errorMessage;
}
