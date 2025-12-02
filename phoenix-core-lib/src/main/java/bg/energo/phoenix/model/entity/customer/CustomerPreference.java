package bg.energo.phoenix.model.entity.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.customer.Preferences;
import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity

@Table(name = "customer_preferences", schema = "customer")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_detail_id", nullable = false)
    private CustomerDetails customerDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferences_id", nullable = false)
    private Preferences preferences;

    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private Status status;

}
