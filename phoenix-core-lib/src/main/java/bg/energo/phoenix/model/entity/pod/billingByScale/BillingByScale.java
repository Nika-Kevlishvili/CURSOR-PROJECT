package bg.energo.phoenix.model.entity.pod.billingByScale;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "billing_by_scale", schema = "pod")
public class BillingByScale extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_by_scale_id_seq",
            sequenceName = "pod.billing_by_scale_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_by_scale_id_seq"
    )
    private Long id;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @Column(name = "billing_power_in_kw")
    private Integer billingPowerInKw;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDateTime invoiceDate;

    @Column(name = "invoice_correction")
    private String invoiceCorrection;

    @Column(name = "correction")
    private Boolean correction;

    @Column(name = "override")
    private Boolean override;

    @Column(name = "basis_for_issuing_invoice")
    private String basisForIssuingInvoice;
    @Column(name = "invoiced")
    private Boolean invoiced;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingByScaleStatus status;

}
