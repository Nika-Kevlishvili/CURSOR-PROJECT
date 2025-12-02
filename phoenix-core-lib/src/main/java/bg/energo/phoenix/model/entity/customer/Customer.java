package bg.energo.phoenix.model.entity.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "customers", schema = "customer")

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_number", nullable = false)
    private Long customerNumber;

    @Column(name = "identifier", nullable = false)
    private String identifier;

    @Column(name = "customer_type", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    @OneToMany(mappedBy = "customer")
    private List<CustomerOwner> customerOwners;

    @Column(name = "last_customer_detail_id")
    private Long lastCustomerDetailId;

    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;

    @Column(name = "is_hard_coded")
    private Boolean isHardCoded;

    @Column(name = "additional_info")
    private String additionalInfo;
}
