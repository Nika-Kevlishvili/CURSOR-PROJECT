package bg.energo.phoenix.model.entity.billing.companyDetails;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Data
@Table(name = "company_banks", schema = "company")
@Entity
@EqualsAndHashCode(callSuper = true)
public class CompanyBank extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "company_banks_id_seq",
            sequenceName = "company.company_banks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "company_banks_id_seq"
    )
    private Long id;

    @Column(name = "company_detail_id")
    private Long companyDetailId;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "iban")
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
