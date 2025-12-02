package bg.energo.phoenix.model.entity.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.customer.GccConnectionType;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.request.customer.ConnectedGroupRequest;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "connected_groups", schema = "customer")

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ConnectedGroup extends BaseEntity {

    @Id
    @SequenceGenerator(name = "connected_groups_id_seq", sequenceName = "customer.connected_groups_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "connected_groups_id_seq")
    private Long id;

    @Column(name = "name", length = 2048, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_types_gcc_id", nullable = false)
    private GccConnectionType gccConnectionType;

    @Column(name = "additional_info", length = 2048)
    private String additionalInfo;

    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private Status status;

    public ConnectedGroup(ConnectedGroupRequest request) {
        if(request!=null){
            this.name = request.getGroupName();
            this.additionalInfo = request.getAdditionalInformation();
            this.status = Status.ACTIVE;
        }
    }
}
