package bg.energo.migration.customer.entity.migrationlog;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@Table(name = "customer_creation_resps", schema = "customer_migration")
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreationResponseLog {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "phoenix_customer_id")
    private Long phoenixCustomerId;

    @Column(name = "error_message")
    private String errorMessage = "";

    @Column(name = "create_date")
    private LocalDateTime createDate = LocalDateTime.now();

}
