package phoenix.core.customer.model.entity.customer;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.entity.nomenclature.customer.Segment;
import phoenix.core.customer.model.enums.customer.Status;

import javax.persistence.*;

@Entity
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
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
    @Type(type = "pgsql_enum")
    @Enumerated(EnumType.STRING)
    private Status status;

}
