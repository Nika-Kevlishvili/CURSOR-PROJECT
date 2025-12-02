package bg.energo.phoenix.model.entity.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity

@Table(name = "customer_segments", schema = "customer")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerSegment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_detail_id", nullable = false)
    private CustomerDetails customerDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = false)
    private Segment segment;

    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private Status status;

}
