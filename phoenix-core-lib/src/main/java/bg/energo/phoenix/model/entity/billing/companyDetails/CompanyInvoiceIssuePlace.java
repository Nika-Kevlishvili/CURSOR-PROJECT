package bg.energo.phoenix.model.entity.billing.companyDetails;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Data
@Table(name = "company_invoice_issue_places", schema = "company")
@Entity
@EqualsAndHashCode(callSuper = true)
public class CompanyInvoiceIssuePlace extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "company_invoice_issue_places_id_seq",
            sequenceName = "company.company_invoice_issue_places_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "company_invoice_issue_places_id_seq"
    )
    private Long id;

    @Column(name = "company_detail_id")
    private Long companyDetailId;

    @Column(name = "invoice_issue_place")
    private String invoiceIssuePlace;

    @Column(name = "invoice_issue_place_transl")
    private String invoiceIssuePlaceTranslated;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
