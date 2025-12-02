package bg.energo.phoenix.model.entity.billing.companyDetails;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Data
@Table(name = "company_communication_addresses", schema = "company")
@Entity
@EqualsAndHashCode(callSuper = true)
public class CompanyCommunicationAddress extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "company_communication_addresses_id_seq",
            sequenceName = "company.company_communication_addresses_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "company_communication_addresses_id_seq"
    )
    private Long id;

    @Column(name = "company_detail_id")
    private Long companyDetailId;

    @Column(name = "address")
    private String address;

    @Column(name = "address_transl")
    private String addressTranslated;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
