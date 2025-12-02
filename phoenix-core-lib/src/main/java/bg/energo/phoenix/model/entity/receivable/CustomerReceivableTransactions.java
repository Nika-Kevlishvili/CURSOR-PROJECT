package bg.energo.phoenix.model.entity.receivable;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import bg.energo.phoenix.model.enums.receivable.SourceObjectType;
import bg.energo.phoenix.model.enums.receivable.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_receivable_transactions", schema = "receivable")
public class CustomerReceivableTransactions extends BaseEntity {

    @Id
    private Long id;

    @Column(name = "operation_date")
    private LocalDateTime operationDate;

    @Column(name = "source_object_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SourceObjectType sourceObjectType;

    @Column(name = "source_object_id")
    private Long sourceObjectId;

    @Column(name = "dest_object_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SourceObjectType destinationObjectType;

    @Column(name = "dest_object_id")
    private Long destinationObjectId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TransactionStatus status;

    @Column(name = "connected_transaction_id")
    private Long connectedTransactionId;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "old_id")
    private Long oldId;

    @Column(name = "operation_context")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OperationContext operationContext;
}
