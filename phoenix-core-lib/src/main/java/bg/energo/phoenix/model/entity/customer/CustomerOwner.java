package bg.energo.phoenix.model.entity.customer;


import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.customer.BelongingCapitalOwner;
import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Table(schema = "customer",name ="customer_owners" )

public class CustomerOwner extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "customer_owners_id_seq", sequenceName = "customer.customer_owners_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_owners_id_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "belonging_capital_owner_id")
    private BelongingCapitalOwner belongingCapitalOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_customer_id")
    private Customer ownerCustomer;

    @Column(name = "additional_info", length = 2048)
    private String additionalInfo;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Status status;

}
