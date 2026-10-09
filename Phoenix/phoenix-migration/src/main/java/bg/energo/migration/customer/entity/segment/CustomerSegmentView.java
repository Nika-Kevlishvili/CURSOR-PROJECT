package bg.energo.migration.customer.entity.segment;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;

@Data
@Entity
@Immutable
@Table(name = "v_customer_segments", schema = "customer_migration")
public class CustomerSegmentView {

    @Id
    private Long id;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "segment_id")
    private Long segmentId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

}

