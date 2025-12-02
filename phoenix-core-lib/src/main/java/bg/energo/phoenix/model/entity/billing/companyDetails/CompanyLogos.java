package bg.energo.phoenix.model.entity.billing.companyDetails;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Data
@Table(name = "company_logos", schema = "company")
@Entity
@EqualsAndHashCode(callSuper = true)
public class CompanyLogos extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "company_logos_id_seq",
            sequenceName = "company.company_logos_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "company_logos_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name ="company_detail_id")
    private Long companyDetailId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;
}
