package bg.energo.migration.customer.entity.migrationlog;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@Table(name = "customer_creation_reqs", schema = "customer_migration")
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreationRequestLog {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id_foreign")
    private Long customerIdForeign;

    @Column(name = "customer_identifier")
    private String customerIdentifier;

    @Column(name = "process_unique_id")
    private UUID processUniqId;

    @Column(name = "payload")
    private String payload;

    @Column(name = "create_date")
    private LocalDateTime createDate = LocalDateTime.now();
}
