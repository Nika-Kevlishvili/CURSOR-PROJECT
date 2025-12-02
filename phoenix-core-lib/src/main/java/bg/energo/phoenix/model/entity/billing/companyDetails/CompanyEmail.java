package bg.energo.phoenix.model.entity.billing.companyDetails;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Data
@Table(name = "company_emails", schema = "company")
@Entity

@EqualsAndHashCode(callSuper = true)
public class CompanyEmail extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "company_emails_id_seq",
            sequenceName = "company.company_emails_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "company_emails_id_seq"
    )
    private Long id;

    @Column(name = "company_detail_id")
    private Long companyDetailId;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
