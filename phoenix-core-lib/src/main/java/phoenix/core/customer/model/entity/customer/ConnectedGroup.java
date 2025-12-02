package phoenix.core.customer.model.entity.customer;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.entity.nomenclature.customer.GccConnectionType;
import phoenix.core.customer.model.enums.customer.Status;

import javax.persistence.*;

@Entity
@Table(name = "connected_groups", schema = "customer")
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Data
@EqualsAndHashCode(callSuper = true)
public class ConnectedGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", length = 2048, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_types_gcc_id", nullable = false)
    private GccConnectionType gccConnectionType;

    @Column(name = "additional_info", length = 2048)
    private String additionalInfo;

    @Column(name = "status", nullable = false)
    @Type(type = "pgsql_enum")
    @Enumerated(EnumType.STRING)
    private Status status;

}
